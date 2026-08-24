# Central de Atendimento da Turma — gRPC

**Disciplina:** (491101) Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
**Unidade:** U1 — Introdução ao Desenvolvimento de Aplicações Distribuídas
**Aluno:** Vitor Augusto Viana Azevedo — matrícula 892281 · Turma T1
**Modalidade:** individual

## Sobre o projeto

Mesmo cenário do laboratório anterior — a central de atendimento da turma — agora com a
comunicação definida por um contrato formal em Protocol Buffers, e o código de rede
gerado a partir dele em vez de escrito à mão.

| Parte | Conteúdo | O que representa no cenário |
|---|---|---|
| A | Transparências em sistemas distribuídos | Revisão conceitual comparando com o lab anterior |
| B | Contrato `.proto` e geração de stubs | Como o aluno pergunta e o monitor responde |
| C | RPC unário — `ConsultarHorario` | Uma pergunta, uma resposta |
| D | RPC com streaming — `AcompanharAvisos` | O mural de avisos, agora entregue por stream |

A comparação com o laboratório anterior está em [`../lab-redes/`](../lab-redes/), no
mesmo repositório — é o que a Parte A exige.

## Estrutura

```
lab-grpc/
├── proto/central.proto              # o contrato, fonte única de verdade
├── java/grpc-central/               # projeto Maven (stubs gerados em target/)
├── python/grpc_central/             # servidor, cliente e stubs gerados
├── evidencias/                      # prints das execuções
├── RESPOSTAS.md                     # as 12 perguntas do roteiro
└── README.md
```

## Portas utilizadas

OFFSET pessoal = **81** (dois últimos dígitos da matrícula 892281), o mesmo do
laboratório anterior.

| Servidor | Porta-base | Porta usada |
|---|---|---|
| gRPC — Java | 50051 | **50132** |
| gRPC — Python | 50061 | **50142** |

## Ambiente de execução

| Item | Valor |
|---|---|
| Sistema operacional | macOS 26.5.2 (arm64) |
| Máquina | MacBook Air — Apple M4 |
| Java | OpenJDK 21 |
| Maven | 3.9.11 |
| Python | 3.13 (venv em `python/grpc_central/.venv`) |
| gRPC | 1.62.2 (Java) · protobuf 3.25.3 |

O roteiro assume Windows com PowerShell. Adaptações usadas, seguindo a mesma convenção
do laboratório anterior:

| Roteiro (Windows) | Equivalente usado (macOS) |
|---|---|
| `Get-Date` | `date` |
| `Copy-Item` | `cp` |
| `pip install` global | `python3 -m venv .venv` + `pip` dentro do venv |

O `pom.xml` recebeu um acréscimo em relação ao do roteiro:
`<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`. Sem ele o Maven usa
o encoding padrão da plataforma, e os acentos de `"Olá"` e `"são"` ficam à mercê do
ambiente.

## Como executar

Cada parte precisa de dois terminais: um para o servidor, outro para o cliente.

```bash
# Gerar os stubs (uma vez, ou sempre que o .proto mudar)
cd java/grpc-central && mvn compile
cd python/grpc_central && source .venv/bin/activate \
  && python -m grpc_tools.protoc -I ../../proto --python_out=. --grpc_python_out=. ../../proto/central.proto

# Java
cd java/grpc-central
mvn compile exec:java -Dexec.mainClass=br.pucminas.labdamd.central.ServidorCentral
mvn compile exec:java -Dexec.mainClass=br.pucminas.labdamd.central.ClienteCentral

# Python
cd python/grpc_central && source .venv/bin/activate
python servidor_central.py
python cliente_central.py
```

O cliente faz as duas chamadas em sequência: o `ConsultarHorario` unário e, logo depois,
o `AcompanharAvisos` com streaming (5 avisos, um a cada 2 segundos).

## Declaração de uso de IA

Conforme a nota de transparência do roteiro, declaro o uso de ferramentas de IA (Claude,
da Anthropic) neste trabalho. Nesta unidade o uso foi mais extenso que no laboratório
anterior, e detalho abaixo para que a divisão fique clara:

**Feito com auxílio de IA:**

- reorganização do repositório em monorepo da disciplina, preservando o histórico;
- redação do `central.proto`, do `pom.xml` e dos quatro programas (servidor e cliente,
  em Java e em Python), a partir dos exemplos do roteiro, com o OFFSET pessoal aplicado;
- scripts auxiliares de automação dos testes de observação;
- organização do histórico de commits: divisão do trabalho por etapa e redação das
  mensagens de commit;
- primeira redação do `RESPOSTAS.md`, revisada e reescrita por mim.

**Feito por mim:**

- toda a execução: builds, geração de stubs, e as quatro demonstrações;
- a captura das quatro evidências em `evidencias/`;
- a condução dos testes de observação e a validação dos resultados — inclusive a
  identificação de um erro de medição no teste de cancelamento do stream em Java, que
  havia produzido um resultado plausível mas incorreto;
- a revisão final e a redação definitiva das respostas.

Comprometo-me a explicar e defender qualquer trecho entregue.
