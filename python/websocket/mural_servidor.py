import asyncio
import websockets
from datetime import datetime

# OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
OFFSET = 81
PORTA = 8888 + OFFSET

# Diferente do multicast, aqui o SERVIDOR mantém a lista de conectados.
alunos = set()


async def tratar_conexao(websocket):
    alunos.add(websocket)
    print(f"[MURAL] Novo aluno conectado. Alunos no mural agora: {len(alunos)}")

    await websocket.send("Bem-vindo ao mural da turma! Você está conectado.")

    # Avisa os demais que alguém entrou
    for outro in alunos:
        if outro is not websocket:
            await outro.send(f"Um novo aluno entrou no mural. Total: {len(alunos)}")

    try:
        async for mensagem in websocket:
            hora = datetime.now().strftime("%H:%M:%S")
            print(f"[MURAL] Aviso recebido: {mensagem}")

            # Difunde para TODOS os conectados, inclusive quem enviou.
            aviso = f"[{hora}] Aviso da turma: {mensagem}"
            websockets.broadcast(alunos, aviso)

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        alunos.discard(websocket)
        print(f"[MURAL] Aluno saiu. Alunos no mural agora: {len(alunos)}")


async def main():
    async with websockets.serve(tratar_conexao, "0.0.0.0", PORTA):
        print(f"[MURAL] Servidor WebSocket ouvindo na porta {PORTA}")
        print("[MURAL] Aguardando conexões... (Ctrl+C para encerrar)")
        print()
        await asyncio.Future()  # roda indefinidamente


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print()
        print("[MURAL] Servidor encerrado.")
