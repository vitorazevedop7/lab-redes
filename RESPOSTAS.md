# Respostas — Roteiro de Laboratório de Redes

**Aluno:** Vitor Augusto Viana Azevedo — matrícula 892281 · Turma T1
**Ambiente:** macOS 26.5.2 · Apple M4 · Java 21.0.9 · Python 3.13 · OFFSET = 81

> As respostas abaixo referem-se ao comportamento observado ao executar os
> programas deste repositório. Os prints correspondentes estão em `evidencias/`.

---

## Parte A — TCP

### 1. O que acontece se você iniciar o cliente antes do servidor? Por que isso ocorre, considerando o funcionamento do TCP?

**O que observei:**

No Java apareceu o erro `java.net.ConnectException: Connection refused` na linha 13, quando tenta criar o `new Socket`. No Python aconteceu praticamente a mesma coisa, dando `ConnectionRefusedError: [Errno 61]` na hora do `connect()`.

```
Exception in thread "main" java.net.ConnectException: Connection refused
        at java.base/java.net.Socket.<init>(Socket.java:324)
        at ClienteTCP.main(ClienteTCP.java:13)
```

```
Traceback (most recent call last):
  File ".../python/tcp/cliente_tcp.py", line 10, in <module>
    cliente.connect((HOST, PORTA))
ConnectionRefusedError: [Errno 61] Connection refused
```

**Explicação:**

Os dois nem chegaram a mandar mensagem. Isso aconteceu porque não tinha nenhum servidor escutando na porta 5081, então o sistema operacional da máquina de destino recusou a conexão. Como o TCP é orientado a conexão, nada é enviado antes do handshake ser concluído — por isso a falha apareceu já no `connect()`, e não na hora do envio.

---

### 2. O TCP garante que as mensagens cheguem na ordem em que foram enviadas. Qual mecanismo do protocolo é responsável por isso?

Essa questão é mais conceitual, então não teve teste no código. O TCP usa números de sequência nos segmentos para conseguir identificar a ordem correta dos dados. Assim, mesmo que eles cheguem fora de ordem, o destino consegue organizar tudo antes de entregar a informação. Além disso, o receptor confirma o recebimento com ACKs e o remetente retransmite o que não for confirmado: os números de sequência sozinhos ordenam, mas é o conjunto sequência + ACK + retransmissão que garante a entrega ordenada e completa.

---

### 3. Na sua implementação, o que aconteceria se dois clientes tentassem se conectar ao mesmo tempo? O código atual suporta isso? Justifique observando o código do servidor.

**O que observei:**

O cliente 2 mostrou `Conectado`, conseguiu mandar `oi`, mas depois ficou travado esperando uma resposta que não veio. No servidor apareceu somente `Recebido: ola`, que era a mensagem do cliente 1. Ou seja: o cliente 2 não recebeu erro nenhum, ele simplesmente ficou bloqueado.

**Justificativa no código:**

Olhando o código, dá pra ver o motivo: o `accept()` aparece só uma vez e está fora de um laço. Depois disso, o `while` fica tratando apenas as mensagens do primeiro cliente. O handshake do segundo cliente foi concluído pelo sistema operacional, e a conexão ficou parada na fila de pendentes do `ServerSocket` esperando um `accept()` que nunca veio — por isso ele imprime `Conectado` (do ponto de vista da rede ele está), mas a aplicação servidora nunca soube que ele existe. Conectado e atendido não são a mesma coisa.

Para suportar vários clientes, o `accept()` teria que estar dentro de um laço e cada conexão aceita ser tratada em uma thread separada.

---

## Parte B — UDP

### 1. No passo 2 da tarefa, o que aconteceu quando você enviou uma mensagem com o servidor desligado? Compare com o que aconteceria em TCP e explique a diferença observada, relacionando com o conceito de "sem conexão".

**O que observei em Java:**

O cliente imprimiu `[UDP] Datagrama enviado (o send() nao garante entrega).` normalmente e, três segundos depois, `[UDP] Nenhuma resposta em 3s.` Nenhuma exceção foi lançada e o programa voltou ao prompt, pronto para enviar outra mensagem.

**O que observei em Python:**

Comportamento idêntico: `[UDP] Datagrama enviado (o sendto() não garante entrega).` seguido de `[UDP] Nenhuma resposta em 3s.` O cliente continuou rodando normalmente.

**Comparação com TCP e explicação:**

No UDP, Java e Python enviaram o datagrama normalmente e depois deram timeout de 3s. Isso acontece porque o UDP não tem handshake. Já no TCP, o cliente falha no `connect()`, com `ConnectException` ou `ConnectionRefusedError`, antes de enviar qualquer dado. O timeout de 3s foi definido no código, não pelo protocolo.

---

### 2. Cite dois exemplos de aplicações reais que usam UDP e explique, para cada uma, por que a confiabilidade do TCP não é essencial (ou até atrapalharia).

Em chamadas de voz, retransmitir um pacote atrasado não ajuda, porque aquele trecho do áudio já passou. Em jogos online acontece o mesmo com posições antigas. Nesses casos, perder um dado é melhor do que aumentar a latência.

---

### 3. No código, o servidor UDP não mantém nenhum registro de "quem está conectado". Isso seria possível de implementar? O que mudaria na arquitetura da aplicação?

No UDP não existe `accept()`. O endereço do cliente vem em cada pacote, então o servidor consegue atender vários clientes diretamente. Para manter uma lista de "conectados", seria preciso usar timeout de inatividade ou heartbeat. Esse controle deixa de ser da camada de transporte e passa a ser responsabilidade da aplicação.

---

## Parte C — Multicast

> **Nota de ambiente:** _(descrever aqui qual `NetworkInterface` funcionou no
> macOS e o que precisou ser ajustado em relação ao código original do roteiro)_

### 1. Qual é a diferença fundamental entre enviar a mesma mensagem para 3 clientes usando unicast repetido 3 vezes e enviar uma única vez via multicast? Pense em termos de tráfego de rede.

_(escrever)_

---

### 2. O que é o TTL (time-to-live) configurado no socket multicast e por que ele é importante para controlar o alcance dos pacotes na rede?

_(escrever)_

---

### 3. Se um dos clientes ficar temporariamente offline e voltar depois, ele recebe os avisos que perdeu? Por quê? Relacione com a arquitetura de comunicação em grupo.

**O que observei:**

_(derrubar um cliente durante o envio dos 5 avisos, subir de novo e verificar se
ele recebe os que perdeu)_

**Explicação:**

_(escrever)_

---

## Parte D — WebSocket

### 1. O WebSocket começa com uma requisição HTTP contendo o cabeçalho `Upgrade: websocket`. O que exatamente "muda" na conexão depois que esse handshake é concluído?

_(escrever)_

---

### 2. Compare o mural via WebSocket (Parte D) com o aviso via Multicast (Parte C). Ambos entregam uma mensagem a vários destinatários — qual a diferença na forma como cada um descobre e alcança os destinatários?

_(escrever — dica: comparar o `getConnections()` do servidor Java da Parte D com
o `joinGroup()` do cliente da Parte C. Quem mantém a lista de destinatários em
cada caso?)_

---

### 3. Por que o WebSocket é mais adequado do que TCP "cru" (como o da Parte A) para este cenário de mural em tempo real, mesmo os dois sendo, no fundo, conexões TCP contínuas?

_(escrever)_

---

## Exercícios extras realizados

_(opcional — registrar aqui o teste cruzado da Parte C, Java↔Python, se funcionar)_
