# Central de Avisos da Turma — Laboratório de Redes

**Disciplina:** (491101) Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
**Unidade:** U0 — Nivelamento de Redes de Computadores e Sistemas Operacionais
**Aluno:** Vitor Augusto Viana Azevedo — matrícula 892281
**Turma:** T1 · **Professor:** Cristiano de Macedo Neto
**Modalidade:** individual

## Sobre o projeto

Implementação da mesma "central de avisos da turma" em quatro protocolos de rede
— TCP, UDP, Multicast e WebSocket — cada um em Java e em Python, totalizando oito
programas. O objetivo é comparar na prática comunicação orientada a conexão, sem
conexão, em grupo e full-duplex em tempo real.

| Parte | Protocolo | O que representa no cenário |
|---|---|---|
| A | TCP | Um aluno pergunta ao monitor e recebe resposta direta (confiável) |
| B | UDP | O mesmo pedido, sem garantia de entrega |
| C | Multicast | O professor avisa todos os alunos conectados de uma vez |
| D | WebSocket | Mural de avisos em tempo real, vários alunos simultâneos |

## Estrutura

```
lab-redes/
├── java/          # implementações em Java (tcp, udp, multicast, websocket)
├── python/        # implementações em Python (tcp, udp, multicast, websocket)
├── evidencias/    # prints comprovando a execução de cada exemplo
├── RESPOSTAS.md   # respostas às 12 perguntas do roteiro
└── README.md
```

## Portas utilizadas

Conforme a seção 3.3 do roteiro, as portas-base foram somadas ao OFFSET pessoal
(dois últimos dígitos da matrícula 892281 → **OFFSET = 81**):

| Parte | Porta-base | Porta usada |
|---|---|---|
| A — TCP | 5000 | 5081 |
| B — UDP | 5001 | 5082 |
| C — Multicast | 4446 | 4527 |
| D — WebSocket (Java) | 8887 | 8968 |
| D — WebSocket (Python) | 8888 | 8969 |

Grupo multicast: `230.0.0.1`.

## Ambiente de execução

| Item | Valor |
|---|---|
| Sistema operacional | macOS 26.5.2 (arm64) |
| Máquina | MacBook Air — Apple M4 |
| Java | OpenJDK 21.0.9 (Homebrew) |
| Maven | 3.9.11 |
| Python | 3.13 (Homebrew) |

O roteiro assume Windows e PowerShell. Este trabalho foi executado em macOS, com
as adaptações correspondentes:

| Roteiro (Windows) | Equivalente usado (macOS) |
|---|---|
| `Get-Date` | `date` |
| `New-Item -ItemType Directory` | `mkdir -p` |
| classpath `"out;lib/*"` | `"out:lib/*"` |
| `Invoke-WebRequest` | `curl -O` |
| `chcp 65001` | desnecessário (UTF-8 por padrão) |

O ajuste de `NetworkInterface` necessário na Parte C está documentado em
`RESPOSTAS.md`.

## Como executar

Cada parte tem seu par servidor/cliente. Rode o servidor em um terminal e o
cliente em outro (dois clientes nas Partes C e D).

```bash
# Parte A — TCP
cd java/tcp   && javac ServidorTCP.java ClienteTCP.java && java ServidorTCP
cd python/tcp && python3 servidor_tcp.py

# Parte B — UDP
cd java/udp   && javac ServidorUDP.java ClienteUDP.java && java ServidorUDP
cd python/udp && python3 servidor_udp.py

# Parte C — Multicast (clientes primeiro, depois o servidor)
cd java/multicast   && javac ServidorMulticast.java ClienteMulticast.java && java ClienteMulticast
cd python/multicast && python3 cliente_multicast.py

# Parte D — WebSocket
cd java/websocket   && mvn compile exec:java -Dexec.mainClass=MuralServidor
cd python/websocket && pip3 install websockets && python3 mural_servidor.py
```

## Declaração de uso de IA

Conforme a nota de transparência do roteiro, declaro o uso de ferramentas de IA
(Claude, da Anthropic) neste trabalho, nas seguintes frentes:

- adaptação dos exemplos de código do roteiro do ambiente Windows para macOS;
- substituição das portas pelo OFFSET pessoal e implementação do comando `hora`
  solicitado na Parte A;
- revisão técnica das respostas escritas por mim.

A execução dos programas, a captura das evidências e a redação das respostas às
doze perguntas são de minha autoria, baseadas no comportamento observado ao rodar
cada exemplo nesta máquina. Comprometo-me a explicar e defender qualquer trecho
entregue.
