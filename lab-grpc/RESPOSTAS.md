# Respostas — Transparências em Sistemas Distribuídos e gRPC

**Aluno:** Vitor Augusto Viana Azevedo — matrícula 892281 · Turma T1
**Ambiente:** macOS 26.5.2 · Apple M4 · Java 21 · Maven 3.9.11 · Python 3.13 · OFFSET = 81
**Portas:** gRPC Java `50132` · gRPC Python `50142`

> As respostas referem-se ao comportamento observado ao executar os programas
> deste repositório. Os prints correspondentes estão em `evidencias/`. As
> referências a arquivos do laboratório anterior apontam para `../lab-redes/`.

---

## Parte A — Transparências em Sistemas Distribuídos

### 4.1 Tarefa — revisão das quatro soluções do laboratório anterior

#### TCP

No TCP, o endereço do servidor (`localhost`) está escrito direto no código. Isso prejudica
a **transparência de localização**, porque o cliente precisa saber exatamente onde o
servidor está.

A comunicação também é feita manualmente: usamos `\n` para indicar o fim da mensagem e
`encode/decode` para converter os dados. Por isso, não existe **transparência de acesso**.

Se o servidor mudasse de máquina, seria necessário alterar o endereço no cliente.

#### UDP

O UDP é parecido com o TCP nesse sentido. O `localhost` também está direto no código,
então existe o mesmo problema de localização.

As mensagens também são tratadas manualmente com `encode/decode`, ou seja, também não há
**transparência de acesso**. Além disso, no UDP o próprio programador precisa controlar o
que representa uma mensagem completa.

Se o servidor mudasse de máquina, o cliente continuaria enviando os dados para o endereço
antigo, podendo até não apresentar erro.

#### Multicast

No Multicast é diferente. O endereço `230.0.0.1` representa um **grupo**, e não uma
máquina específica. O cliente só entra nesse grupo e recebe as mensagens enviadas para
ele, sem precisar saber qual máquina está enviando.

Por isso, ele possui **transparência de localização**, embora o tratamento do conteúdo das
mensagens continue sendo manual — ou seja, sem transparência de acesso.

Se o servidor mudasse de máquina, o cliente continuaria funcionando sem alteração, desde
que o novo servidor continuasse enviando para o mesmo grupo e estivesse dentro do alcance
da rede.

#### WebSocket

No WebSocket, o endereço também está no código, usando `ws://localhost`. Então, se o
servidor mudar, ainda precisamos alterar a URL.

Por outro lado, ele melhora a **transparência de acesso**, porque o próprio WebSocket
controla o início e o fim das mensagens. O programador ainda interpreta o conteúdo, mas
não precisa controlar manualmente o fluxo de bytes como no TCP.

#### Resumo

| Solução | Endereço | Transparência de acesso | Funciona se o servidor mudar? |
|---|---|---|---|
| TCP | `localhost` | Não | Não |
| UDP | `localhost` | Não | Não |
| Multicast | `230.0.0.1` | Não | **Sim** |
| WebSocket | `ws://localhost` | Parcial | Não |

No geral, o **Multicast é o único que continuaria funcionando sem alterar o cliente**,
porque ele usa o endereço de um grupo e não de uma máquina específica. Nas outras
soluções, o endereço está diretamente no código e precisaria ser alterado caso o servidor
mudasse.

### 4.3 Perguntas

#### 1. Qual das 8 transparências é a mais visível para o programador que consome um serviço remoto?

A transparência de acesso, porque é a que mais muda a forma como eu programo.

Por exemplo, no TCP eu preciso trabalhar diretamente com socket, enviar dados e
interpretar a resposta. Já no gRPC posso simplesmente chamar um método como:

```java
stub.consultarHorario(pergunta);
```

As outras transparências, como falha, replicação ou migração, estão mais relacionadas ao
que acontece por trás do sistema e normalmente não aparecem diretamente no código.

#### 2. Transparência total é sempre desejável?

Não. Esconder completamente que uma chamada é remota pode até causar problemas.

Por exemplo, se eu fizer 500 chamadas gRPC dentro de um `for`, o código pode parecer
simples, mas na prática serão 500 comunicações pela rede, o que pode deixar o programa
muito lento.

Também existem falhas que só fazem sentido em sistemas distribuídos. O servidor pode
receber e executar uma operação, mas a resposta pode não chegar ao cliente. Se ele
simplesmente repetir a chamada, pode acabar executando a mesma operação duas vezes.

Então, é bom o gRPC facilitar o acesso, mas ainda deixar claro que existem problemas como
latência, falhas e timeout.

#### 3. Comparação entre o cliente TCP e o cliente gRPC

No TCP eu precisava lidar mais diretamente com a rede, criando o socket, enviando dados e
interpretando a resposta.

No gRPC isso fica bem mais simples, porque eu envio um objeto e recebo outro:

```java
RespostaHorario resposta = stub.consultarHorario(pergunta);
```

Assim, consigo focar mais na lógica do problema e menos na comunicação.

Isso se relaciona com a transparência de acesso, porque o gRPC esconde os detalhes da
comunicação e permite acessar o serviço remoto de forma parecida com uma chamada local.

Mesmo assim, a chamada continua sendo remota. Se o servidor estiver desligado, ela falha,
e também pode levar mais tempo do que uma chamada local.

---

## Parte B — Protocol Buffers e o contrato do serviço

#### 1. Qual a vantagem do contrato explícito e gerado automaticamente?

No TCP, o formato das mensagens dependia de uma combinação entre cliente e servidor. Se um
lado esperasse `"hora"` e o outro enviasse `"horario"`, o problema só apareceria durante a
execução.

Com o `.proto`, existe um contrato único dizendo quais mensagens e operações existem.
Isso traz algumas vantagens:

- cliente e servidor seguem a mesma estrutura;
- vários erros aparecem já na compilação;
- o contrato pode ser versionado no Git;
- fica mais fácil alterar e evoluir o sistema.

#### 2. O que o mesmo `.proto` gerando Java e Python sugere?

Mostra que cliente e servidor não precisam usar a mesma linguagem. O `.proto` funciona
como o contrato entre eles. Assim, posso ter um servidor em Python e um cliente em Java,
por exemplo, e os dois continuam conseguindo se comunicar.

Isso também significa que o servidor poderia ser refeito em outra linguagem sem
necessariamente alterar o cliente, desde que o contrato continue o mesmo.

#### 3. Onde ficam definidas as operações no código gerado?

No Python, as operações aparecem no `CentralAtendimentoStub`, por exemplo:

```python
self.ConsultarHorario = channel.unary_unary(...)
self.AcompanharAvisos = channel.unary_stream(...)
```

No Java, elas aparecem no `CentralAtendimentoGrpc.java`, através dos métodos e descritores
gerados. Também consegui identificar a `CentralAtendimentoImplBase`, que é justamente a
classe utilizada para implementar o servidor.

---

## Parte C — RPC unário: ConsultarHorario

#### 1. O que acontece por baixo dos panos?

Quando faço:

```java
stub.consultarHorario(pergunta);
```

o gRPC realiza várias etapas automaticamente:

1. O objeto é serializado pelo Protocol Buffers.
2. Os dados são enviados pela rede usando HTTP/2.
3. O servidor identifica qual método deve executar.
4. Os dados são transformados novamente em um objeto.
5. O método do servidor é executado.
6. A resposta é serializada e enviada de volta.

A principal diferença é que eu não preciso trabalhar diretamente com bytes, delimitadores
ou encoding.

#### 2. Comparação com o TCP

No TCP eu precisava montar e interpretar as mensagens manualmente. Por exemplo:

```python
cliente.sendall((mensagem + "\n").encode("utf-8"))
```

No gRPC, essa responsabilidade fica com o código gerado a partir do `.proto`.

Outra diferença importante é que no TCP o servidor precisava interpretar o conteúdo da
mensagem para descobrir o que fazer. No gRPC, a própria chamada já identifica qual método
deve ser executado.

#### 3. E se o servidor estiver desligado?

Nos testes, tanto Java quanto Python retornaram o status:

```
UNAVAILABLE
```

No Python, percebi também que criar o `channel` não gera erro imediatamente. O erro só
aparece quando realmente tento fazer uma chamada RPC.

No Java aconteceu algo parecido, mas a exceção aparece de outra forma por causa da
linguagem e do Maven.

Isso mostra um limite da transparência de acesso: a chamada parece um método normal, mas
ainda pode falhar porque depende de outro computador ou processo.

---

## Parte D — RPC com streaming de servidor: AcompanharAvisos

#### 1. Como fazer vários clientes receberem os mesmos avisos?

Atualmente cada cliente recebe seu próprio stream. Ou seja, cada chamada inicia uma nova
sequência de avisos.

Para funcionar como um mural de verdade, o servidor precisaria manter uma lista de
clientes conectados e gerar cada aviso apenas uma vez, enviando depois para todos eles.

No Multicast isso era feito pela própria rede. No gRPC, o servidor precisa controlar os
clientes e enviar uma cópia para cada conexão.

#### 2. `StreamObserver`/`onNext()` versus `yield`

No Python, achei o `yield` mais simples e fácil de entender:

```python
yield aviso
```

O código parece uma função que vai produzindo vários resultados.

No Java, o `StreamObserver` é mais detalhado:

```java
observador.onNext(aviso);
observador.onCompleted();
```

Ele é mais verboso, mas deixa mais claro quando uma mensagem é enviada e quando o stream
termina.

Então, para escrever de forma simples eu prefiro Python. Para entender melhor o
funcionamento do protocolo, achei o Java mais explícito.

#### 3. O que acontece se o cliente fechar a conexão no meio?

Para conseguir observar isso, usei temporariamente uma versão do servidor que imprime uma
linha a cada aviso gerado, mostrando se a conexão ainda estava ativa. O código entregue
neste repositório é o original, sem essas linhas.

Nos meus testes, tanto Java quanto Python demoraram um pouco para perceber que o cliente
tinha desconectado.

No Python, quando o cliente deixou de consumir o stream, o gerador acabou parando de
produzir novos avisos.

No Java foi diferente: o servidor continuou executando o laço mesmo depois de detectar que
o cliente tinha sido cancelado.

Por isso, no Java é importante verificar algo como:

```java
isCancelled()
```

dentro do laço, principalmente se cada item exigir processamento pesado.

Esse teste mostra um limite da transparência de falha. O gRPC facilita bastante o
tratamento da comunicação, mas o servidor ainda precisa estar preparado para situações
como um cliente desconectar no meio da operação.
