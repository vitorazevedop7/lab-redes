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

**Endereço no código do cliente?** Sim, literal. Em `../lab-redes/python/tcp/cliente_tcp.py`
a linha 6 traz `HOST = "localhost"`, e em `ClienteTCP.java` a linha 10 traz
`String host = "localhost"`. Isso **prejudica a transparência de localização**: o
"onde" está congelado no código-fonte, e não em configuração, DNS ou serviço de
nomes. Quem escreveu o cliente precisou saber onde o servidor mora.

**Montagem e parsing manual?** Sim, dos dois lados. O cliente faz
`cliente.sendall((mensagem + "\n").encode("utf-8"))` e lê a resposta com
`arquivo.readline().strip()`. O `"\n"` é o delimitador de mensagem, combinado por
convenção; o `.encode`/`.decode` é serialização escrita à mão. Isso é **ausência de
transparência de acesso** — não é meio-termo, porque não existe nenhuma noção de
"operação": existe um fluxo de bytes que os dois lados concordaram em interpretar.

**Se o servidor mudasse de máquina?** O cliente quebraria. Seria preciso editar a
constante e recompilar (Java) ou reeditar o arquivo (Python).

#### UDP

**Endereço no código?** Sim, igual ao TCP: `HOST = "localhost"` na linha 6 de
`cliente_udp.py` e `String host = "localhost"` na linha 11 de `ClienteUDP.java`,
resolvido depois com `InetAddress.getByName(host)`. Mesmo prejuízo à transparência
de localização.

**Montagem e parsing manual?** Sim: `cliente.sendto(mensagem.encode("utf-8"), (HOST, PORTA))`
e, na volta, `dados.decode("utf-8")`. Ausência de transparência de acesso, pelo
mesmo motivo do TCP — com um agravante: como o UDP não tem fluxo, o programador
também assume a responsabilidade de decidir o que é uma "mensagem completa".

**Se o servidor mudasse de máquina?** Quebraria igual — e de forma mais traiçoeira,
porque o `sendto()` continuaria "funcionando" sem erro nenhum, mandando datagramas
para um endereço que não atende mais.

#### Multicast

**Endereço no código?** Sim, mas é um endereço de natureza diferente:
`GRUPO = "230.0.0.1"` (linha 8 de `cliente_multicast.py`, linha 10 de
`ClienteMulticast.java`). O que está escrito no cliente **não é o endereço de uma
máquina, é o de um grupo**. O cliente não sabe quem envia nem de onde — ele se
inscreve num canal lógico. Essa é a única das quatro soluções com um grau real de
**transparência de localização**.

**Montagem e parsing manual?** Sim, permanece: `new String(pacote.getData(), ...)` e
`dados.decode('utf-8')`. A transparência conquistada aqui é de localização, não de
acesso.

**Se o servidor mudasse de máquina?** **Sobreviveria sem alterar o código do cliente**,
desde que a nova máquina esteja na mesma rede e continue enviando para
`230.0.0.1:4527`. É exatamente o que o desacoplamento por grupo compra. A ressalva é
que esse alcance é limitado pelo TTL e pela topologia da rede local — o grupo não
atravessa a internet sem infraestrutura adicional.

#### WebSocket

**Endereço no código?** Sim, mas na forma de URL: `URL = f"ws://localhost:{PORTA}"`
(linha 10 de `mural_cliente.py`) e `String url = "ws://localhost:" + PORTA;`
(linha 16 de `MuralCliente.java`). Continua sendo uma string literal no fonte, então
o prejuízo à transparência de localização permanece — mas a **forma** é melhor: uma
URL é um ponto único de indireção, e trocar `localhost` por um nome DNS passa a ser
mudança de configuração, não reescrita de lógica.

**Montagem e parsing manual?** Aqui é **meio-termo**. O enquadramento de mensagens
deixou de ser problema do programador: o WebSocket entrega mensagem inteira, não
fluxo de bytes, então o `"\n"` combinado do TCP sumiu. Mas o *conteúdo* continua
texto solto interpretado à mão (`linha.strip().lower() == "sair"`). Ganhou-se o
envelope, não o significado.

**Se o servidor mudasse de máquina?** Quebraria, mas seria o remendo mais barato dos
quatro — só a URL muda, e ela já é um ponto único.

#### Resumo

| Solução | Endereço no fonte | Transparência de acesso | Sobrevive à troca de máquina? |
|---|---|---|---|
| TCP | `localhost` | Ausente | Não |
| UDP | `localhost` | Ausente | Não (e falha em silêncio) |
| Multicast | grupo `230.0.0.1` | Ausente | **Sim** |
| WebSocket | URL `ws://localhost` | Meio-termo | Não (mas troca barata) |

Só o Multicast sobrevive sem alterar código-fonte, e por um motivo que não é mérito
do programador: o endereçamento por grupo já embute a indireção. Nenhuma das quatro
lê o endereço de configuração externa, variável de ambiente ou serviço de nomes.

### 4.3 Perguntas

#### 1. Qual das 8 transparências é a mais visível para o programador que consome um serviço remoto?

A **transparência de acesso**. Ela é a única que muda a forma como o código é
escrito, e não apenas o que acontece por baixo dele.

As outras sete descrevem coisas que a infraestrutura faz e o programador idealmente
não percebe: se há três réplicas, se um nó caiu e voltou, se o recurso migrou de
datacenter. Já a transparência de acesso é justamente a que decide se o programador
escreve `stub.consultarHorario(pergunta)` ou se escreve `socket.send(...)` seguido
de `readline()` e parsing. Ela é visível porque ela *é* a interface — as demais são
propriedades do que está atrás da interface.

Vale a distinção: mais visível não é o mesmo que mais importante. A transparência de
falha provavelmente importa mais para a robustez do sistema; ela só é menos visível
porque, quando funciona, não aparece.

#### 2. Transparência total é sempre desejável?

Não. O caso clássico é esconder o custo de uma chamada remota a ponto de o
programador tratá-la como local.

Exemplo concreto: um cliente precisa do horário de 500 alunos e escreve
`for aluno in alunos: stub.ConsultarHorario(aluno)`. O código parece um laço barato
sobre uma função local. Na prática são 500 idas e voltas pela rede — se cada uma
custar 40 ms, o laço leva 20 segundos, e nada na sintaxe da chamada denuncia isso.
Se a operação fosse visivelmente remota, o programador provavelmente teria pedido
uma chamada em lote.

O segundo caso é o tratamento de falhas. Uma chamada local ou funciona ou lança
exceção; uma chamada remota tem um estado que não existe localmente — o pedido pode
ter sido executado no servidor e a resposta ter se perdido na volta. Quem acredita
que está chamando uma função local não escreve retry idempotente, porque no mundo
local repetir uma chamada que falhou é inofensivo. Aqui, repetir pode significar
executar a operação duas vezes.

É o argumento de *A Note on Distributed Computing* (Waldo et al., 1994): a diferença
entre chamada local e remota não é de grau, é de natureza — latência, falha parcial
e concorrência não desaparecem por serem escondidas. O ideal é transparência de
acesso na *sintaxe*, mas honestidade na *semântica*: o gRPC faz isso ao obrigar o
tratamento de `StatusRuntimeException`/`RpcError` e ao expor deadlines.

#### 3. Comparação entre o cliente TCP e o cliente gRPC

O `ClienteTCP` me obrigava a **pensar em rede**. Para pedir o horário eu precisava
saber que existia um socket, abrir a conexão, montar a string, escolher um
delimitador (`"\n"`), enviar bytes, ler bytes de volta e interpretar o texto. Nada
disso é sobre horário — é tudo infraestrutura, e ela ocupava a maior parte do código.

O `ClienteCentral` me deixa **pensar no problema**. A linha que importa é uma só:

```java
RespostaHorario resposta = stub.consultarHorario(pergunta);
```

Passo um objeto, recebo um objeto, e o `resposta.getMensagem()` é um campo tipado, não
uma string que eu preciso fatiar. O socket, o delimitador, a serialização e o
roteamento continuam existindo — só deixaram de ser problema meu.

Isso é **transparência de acesso**: o recurso remoto passa a ser acessado da mesma
forma que um recurso local. É a transparência que eu apontei na pergunta 1 como a mais
visível para quem consome um serviço, e aqui dá para medir a diferença em linhas de
código.

Duas ressalvas que a Parte C e a Parte D me mostraram, e que impedem de chamar isso de
transparência total:

- **A falha não se esconde.** Com o servidor desligado, a chamada que "parece local"
  estoura um erro que nenhuma chamada local pode ter: o outro lado não existe. É o
  argumento da pergunta 2 saindo do hipotético.
- **O tempo não se esconde.** No `AcompanharAvisos`, o `for` do cliente parece iterar
  uma coleção comum, mas cada volta espera 2 segundos porque o dado ainda **não
  existe** — está sendo produzido do outro lado enquanto eu itero. Nenhuma coleção
  local se comporta assim.

Ou seja: o gRPC entrega transparência de acesso na sintaxe, mas mantém a semântica
honesta — e é justamente essa combinação que faz dele uma ferramenta utilizável, em
vez de uma ilusão perigosa.

---

## Parte B — Protocol Buffers e o contrato do serviço

#### 1. Qual a vantagem do contrato explícito e gerado automaticamente?

No laboratório anterior o formato existia, mas só na cabeça de quem escreveu os dois
lados. O cliente TCP mandava `hora` e o servidor comparava com a string `"hora"`; se
alguém renomeasse para `horario` em um dos lados, nada acusaria o erro — o programa
compilaria, subiria, e só na execução o servidor responderia "comando desconhecido".
O contrato era verificado por conversa, e o custo de quebrá-lo aparecia tarde.

Com o `central.proto`, três coisas mudam:

- **Fonte única de verdade.** Existe um arquivo que define o que é uma
  `PerguntaHorario`. Não há duas definições podendo divergir.
- **Erro antecipado.** Se eu escrever `pergunta.getNome()` em vez de
  `getNomeAluno()`, o build falha. O que antes era bug de execução vira erro de
  compilação.
- **O contrato é versionável.** Ele está no Git, tem histórico, e a numeração dos
  campos (`= 1`, `= 2`) permite evoluir a mensagem sem quebrar quem já usa a versão
  antiga — algo impossível com string posicional montada à mão.

#### 2. O que o mesmo .proto gerando Java e Python sugere?

Sugere que, num sistema distribuído real, **a fronteira entre equipes é o contrato, não
a linguagem**. Cada time gera o stub na linguagem que preferir a partir do mesmo
arquivo, e a interoperabilidade é consequência do processo, não de esforço de
integração.

O efeito prático mais forte é a substituibilidade: como o cliente conhece o serviço
apenas pelo `.proto`, o servidor Python deste laboratório poderia ser reescrito em
Java (ou Go, ou Rust) sem que uma linha do cliente mudasse. A linguagem vira detalhe
de implementação — o que é uma forma de transparência de acesso em escala
organizacional.

#### 3. Onde ficam definidas as operações no código gerado?

Consegui identificar nas duas linguagens, e o contraste entre elas é informativo.

**Python** (`python/grpc_central/central_pb2_grpc.py`) — no construtor de
`CentralAtendimentoStub`, cada operação vira um atributo:

```python
self.ConsultarHorario = channel.unary_unary(...)   # linha 37
self.AcompanharAvisos = channel.unary_stream(...)  # linha 42
```

Os nomes `unary_unary` e `unary_stream` já revelam o estilo de cada RPC: uma
resposta contra várias. Do lado servidor, `add_CentralAtendimentoServicer_to_server`
monta o mesmo par com `grpc.unary_unary_rpc_method_handler` e
`grpc.unary_stream_rpc_method_handler`.

**Java** (`CentralAtendimentoGrpc.java`, gerado em `target/generated-sources/`) — as
operações aparecem primeiro como `MethodDescriptor`, e depois como métodos da classe
base que o servidor estende:

```java
SERVICE_NAME = "central.CentralAtendimento";                    // linha 15
.setFullMethodName(generateFullMethodName(SERVICE_NAME, "ConsultarHorario"))  // linha 35
public abstract static class CentralAtendimentoImplBase          // linha 152
public void consultarHorario(...)                                // linha 181
public void acompanharAvisos(...)                                // linha 192
```

Reconheci `CentralAtendimentoImplBase` — é exatamente a classe que o meu
`ServidorCentral.CentralAtendimentoImpl` estende.

Um detalhe que achei revelador: o método em Java se chama `consultarHorario`
(minúsculo), seguindo a convenção da linguagem, mas o nome que trafega na rede
continua sendo `central.CentralAtendimento/ConsultarHorario`. O gerador adapta o
código ao estilo de cada linguagem sem mexer no contrato — que é justamente o motivo
de Java e Python conseguirem conversar.

---

## Parte C — RPC unário: ConsultarHorario

#### 1. O que acontece "por baixo dos panos" entre a chamada e o `return` no servidor?

A linha `stub.consultarHorario(pergunta)` tem uma aparência enganosa. Entre ela e o
`return` do servidor acontecem, no mínimo:

1. **Serialização.** O stub converte o objeto `PerguntaHorario` para o formato
   binário do protobuf — não é o texto `Vitor`, é o campo de número 1 codificado com
   sua tag e seu comprimento. É exatamente o trabalho que no laboratório anterior eu
   escrevi à mão com `getBytes()` e `encode("utf-8")`.

2. **Transporte e roteamento.** O canal abre (ou reaproveita) uma conexão HTTP/2 com
   `localhost:50132` e cria um *stream* novo dentro dela. Os bytes vão num frame de
   dados, acompanhados de cabeçalhos que carregam o nome completo do método,
   `central.CentralAtendimento/ConsultarHorario`. É esse cabeçalho que diz ao servidor
   qual método executar.

3. **Despacho do lado do servidor.** O gRPC lê o frame, identifica o método pelo
   cabeçalho, desserializa os bytes de volta num objeto `PerguntaHorario` e chama o
   meu método — numa thread do pool do servidor, não na thread que estava escutando a
   porta. Foi por isso que precisei declarar um `ThreadPoolExecutor` no Python e não
   precisei escrever nada sobre threads no Java.

4. **A volta.** O `observador.onNext(resposta)` serializa de novo e envia; o
   `onCompleted()` encerra o stream com um *trailer* de status `OK`; o stub
   bloqueante desbloqueia e devolve o objeto pronto.

Vale registrar o que **não** acontece: nenhuma linha do meu código toca em byte,
delimitador de mensagem ou encoding.

#### 2. Comparação com o `ClienteTCP` do laboratório anterior

No TCP, "montar a mensagem" era o `saida.println(linha)` do Java e o
`cliente.sendall((mensagem + "\n").encode("utf-8"))` do Python — inclusive o `"\n"`,
que era o delimitador combinado por convenção. "Interpretar a resposta" era o
`entrada.readLine()` / `arquivo.readline().strip()` no cliente, e no servidor era o
`if` que comparava a string recebida com `"hora"`.

Agora esse trabalho todo é feito pelo código gerado a partir do `central.proto`: as
classes `PerguntaHorario` e `RespostaHorario` cuidam da serialização, e o
`CentralAtendimentoGrpc` cuida do despacho.

A diferença que eu acho mais importante não é *quem* faz, é *onde a informação mora*.
No TCP, qual operação eu queria era **conteúdo** da mensagem: a string `"hora"` viajava
no corpo e o servidor precisava lê-la e decidir. No gRPC, qual operação eu quero é
**metadado do protocolo**: vai no cabeçalho HTTP/2, e o servidor roteia sem inspecionar
o corpo. Deixou de ser interpretação de texto e virou roteamento.

#### 3. E se o servidor estiver desligado?

**Python** — a chamada levanta `grpc._channel._InactiveRpcError`:

```
status = StatusCode.UNAVAILABLE
details = "failed to connect to all addresses; last error: UNKNOWN:
           ipv4:127.0.0.1:50142: Failed to connect to remote host: Connection refused"
```

O detalhe que eu não esperava: o erro **não** aparece no `grpc.insecure_channel(...)`,
e sim na linha da chamada. O canal gRPC é preguiçoso — criá-lo não abre conexão
nenhuma, ele só tenta conectar quando há um RPC de verdade para enviar. É diferente do
TCP do laboratório anterior, onde o `new Socket(...)` já falhava com
`ConnectException` porque o construtor faz o handshake.

**Java** — o mesmo status, embrulhado pelo Maven:

```
[ERROR] Failed to execute goal ...exec-maven-plugin:3.1.0:java (default-cli) on project
grpc-central: An exception occurred while executing the Java class.
UNAVAILABLE: io exception: Connection refused: localhost/[0:0:0:0:0:0:0:1]:50132
```

Duas coisas que eu não esperava aqui.

A primeira: o status é **exatamente o mesmo** das duas linguagens, `UNAVAILABLE`. O
código de erro faz parte do contrato do gRPC, não da linguagem — muda só a embalagem
(`_InactiveRpcError` no Python, `StatusRuntimeException` no Java, que por sua vez veio
embrulhada num `MojoExecutionException` porque rodei via `mvn exec:java`).

A segunda, e mais interessante: `localhost/[0:0:0:0:0:0:0:1]`. O cliente Java resolveu
`localhost` para o **loopback IPv6** (`::1`), enquanto o Python foi para
`127.0.0.1`. É o mesmo literal `"localhost"` escrito no código-fonte resultando em dois
endereços diferentes, e reforça o que eu respondi na Parte A: o `"localhost"` hardcoded
não é sequer uma resposta precisa para "onde está o servidor" — ele delega essa decisão
para a resolução de nomes da plataforma, e cada runtime decide de um jeito.

O ponto conceitual é o mesmo dos dois lados: a falha remota **não** consegue se
disfarçar de falha local. Por mais que `stub.consultarHorario(pergunta)` pareça uma
chamada de método comum, ela pode falhar por um motivo que nenhuma chamada local tem —
o outro lado não existir. É o limite prático da transparência de acesso, e conecta
com a resposta que dei na pergunta 2 da Parte A.

---

## Parte D — RPC com streaming de servidor: AcompanharAvisos

#### 1. Como fazer vários clientes receberem os mesmos avisos ao mesmo tempo?

Do jeito que está, **cada cliente tem o seu próprio stream particular**. O método
`AcompanharAvisos` é executado uma vez por chamada, e cada execução roda o seu próprio
laço de 1 a 5. Se dois alunos se inscreverem com 30 segundos de diferença, cada um
recebe o "Aviso #1" no seu próprio instante — não são os mesmos avisos, são duas
sequências independentes que por acaso têm o mesmo texto.

Para virar um mural de verdade, o servidor teria que parar de **gerar** e passar a
**distribuir**:

- Manter um registro dos inscritos ativos — em Java, uma coleção de
  `StreamObserver<Aviso>`; em Python, uma fila por cliente.
- O método RPC deixaria de produzir avisos. Ele registraria o cliente e ficaria vivo
  sem chamar `onCompleted()` (Java) ou consumindo a sua fila com `yield` (Python).
- Uma thread separada produziria cada aviso **uma vez** e faria o fan-out para todos os
  inscritos do registro.
- E precisaria de duas coisas que hoje não existem: sincronização, porque o registro
  passa a ser mexido por várias threads ao mesmo tempo, e remoção do inscrito quando
  ele cai — senão o servidor acumula observadores mortos.

A comparação com a Parte C do laboratório anterior é o ponto interessante. **No
Multicast, o fan-out era da rede**: o servidor mandava um datagrama só, e a
infraestrutura replicava para quem estivesse no grupo. **No gRPC, o fan-out é da
aplicação**: o servidor mantém N conexões e envia N cópias, uma por cliente.

Cada um paga um preço diferente. O multicast economiza banda mas não sai da rede local
sem infraestrutura extra, não sabe quem está ouvindo e não garante entrega. O gRPC
gasta uma conexão por cliente, mas atravessa a internet, sabe exatamente quem está
inscrito e herda a confiabilidade do TCP. Não é um melhor que o outro — é um
diagnóstico diferente do mesmo problema.

#### 2. `StreamObserver`/`onNext()` versus função geradora com `yield`

**O gerador do Python é mais natural de ler.** O método parece uma função comum que
produz uma sequência: o `for` é o laço de verdade, o `yield` é "aqui sai um item", e o
fim do stream é simplesmente o fim do laço. Quem nunca viu gRPC entende o que o código
faz.

**O `StreamObserver` do Java é mais explícito sobre o protocolo.** Ele é mais verboso,
mas os três métodos — `onNext()`, `onCompleted()`, `onError()` — mapeiam um-para-um
com o que realmente acontece no fio: mandar um item, fechar com status OK, fechar com
erro. Nada fica implícito.

E é aí que eu mudo de ideia sobre qual eu prefiro, dependendo do critério. Para
escrever rápido, o Python. Para **enxergar o protocolo**, o Java — porque no Python o
encerramento do stream é invisível: ele acontece quando o gerador se esgota, e não há
nenhuma linha escrita dizendo "acabou". No Java eu sou obrigado a escrever
`observador.onCompleted()`, e a existência dessa linha me lembra que existe um estado
de "stream aberto" que precisa ser fechado. O tratamento de erro segue a mesma lógica:
o Java me forçou a escrever o `catch` com `observador.onError(e)`; o Python deixaria
uma exceção subir sem que eu pensasse a respeito.

A pergunta 3 acabou reforçando esse ponto por um caminho que eu não esperava: como o
Java não interrompe o meu laço quando o cliente some, a explicitação do protocolo deixa
de ser preferência de estilo e vira necessidade — é o programador que precisa perguntar
`isCancelled()`, porque ninguém vai perguntar por ele.

#### 3. O que acontece se o cliente fechar a conexão no meio dos 5 avisos?

Testei nas duas linguagens matando o cliente com `SIGKILL` durante o aviso #3 — o mais
próximo de fechar o terminal na cara, sem encerramento limpo. Para conseguir observar,
usei uma versão **temporariamente instrumentada** do servidor, que imprime uma linha por
aviso com o estado da conexão. O código entregue neste repositório é o original, sem a
instrumentação.

**Python:**

```
[obs] gerando aviso #1 | conexao ativa = True
[obs] gerando aviso #2 | conexao ativa = True
[obs] gerando aviso #3 | conexao ativa = True
[obs] gerando aviso #4 | conexao ativa = False
```

**Java:**

```
[obs] gerando aviso #1 | cancelado = false
[obs] gerando aviso #2 | cancelado = false
[obs] gerando aviso #3 | cancelado = false
[obs] gerando aviso #4 | cancelado = true
[obs] gerando aviso #5 | cancelado = true
[obs] laço terminou naturalmente (chegou ao aviso #5)
```

**O que os dois têm em comum: a detecção é atrasada.** Os dois seguiram gerando avisos
depois que o cliente já estava morto, e só perceberam no aviso #4 — uma iteração inteira
(2 segundos) depois. Faz sentido: o servidor não é notificado no instante da morte, ele
só descobre quando tenta escrever e a escrita falha.

**Onde eles diferem — e essa é a parte que me surpreendeu.** O Python **parou**: o aviso
#5 nunca foi gerado e a mensagem de fim de laço nunca apareceu. O runtime do gRPC deixou
de puxar itens do gerador, e isso encerrou o laço. Já o Java **foi até o fim**: gerou os
avisos #4 e #5 sabendo que estava cancelado, e imprimiu a mensagem de conclusão. Eu tinha
colocado um `catch (RuntimeException)` no teste esperando ver uma exceção, e não veio
nenhuma — o `onNext()` sobre um stream cancelado simplesmente **descarta a mensagem em
silêncio**.

A consequência prática é concreta: no Java, um servidor que faça trabalho caro por item
continua queimando CPU e memória para clientes que já foram embora, e nada avisa. Por isso
existe o `isCancelled()` — e por isso, num serviço de verdade, ele (ou um
`setOnCancelHandler`) precisa ser checado dentro do laço. No Python, o protocolo de
geradores dá isso de graça: quem consome parou de pedir, então a produção para sozinha.

**A conexão com a teoria:** isso é transparência de falha mostrando exatamente onde ela
acaba. O gRPC esconde a falha do lado de quem *chama* — o cliente morto não deixa o
servidor inconsistente, e o stream é encerrado sem intervenção. Mas ele não esconde a
falha de quem *serve*: o servidor precisa saber que existe um cliente que pode sumir, e
programar defensivamente para isso. É o mesmo limite que apareceu na Parte C com o
servidor desligado, visto do outro lado da conexão.

**Nota de método:** o meu primeiro teste deu `cancelado = false` nas cinco iterações, o
que parecia indicar que o Java nunca detecta o cancelamento. Era erro de medição: eu
matava o cliente com um `sleep` de tempo fixo que, somado ao tempo de build do Maven,
disparava *depois* do stream já ter terminado. Refiz sincronizando a morte do cliente com
o log do servidor em vez de com o relógio. Registro isso porque o resultado errado era
plausível o bastante para eu ter aceitado sem conferir.