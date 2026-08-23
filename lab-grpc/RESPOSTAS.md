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

*(a responder após concluir as Partes C e D)*

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

*(a responder após a execução)*

---

## Parte D — RPC com streaming de servidor: AcompanharAvisos

*(a responder após a execução)*
