import asyncio
import sys
import websockets

# OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
OFFSET = 81
PORTA = 8888 + OFFSET

nome = sys.argv[1] if len(sys.argv) > 1 else "Aluno"
URL = f"ws://localhost:{PORTA}"


async def receber(websocket):
    """Fica ouvindo o servidor em paralelo com a digitação."""
    try:
        async for mensagem in websocket:
            print(f"  >> {mensagem}")
    except websockets.exceptions.ConnectionClosed:
        pass


async def enviar(websocket):
    """Lê o teclado sem bloquear o laço de eventos."""
    loop = asyncio.get_running_loop()
    while True:
        # input() é bloqueante: rodá-lo em um executor evita travar o receber().
        linha = await loop.run_in_executor(None, input)
        if linha.strip().lower() == "sair":
            break
        await websocket.send(f"{nome} diz: {linha}")


async def main():
    async with websockets.connect(URL) as websocket:
        print(f"[MURAL] {nome} conectado em {URL}")
        print("[MURAL] Digite um aviso e tecle Enter. 'sair' encerra.")
        print()

        tarefa_receber = asyncio.create_task(receber(websocket))
        tarefa_enviar = asyncio.create_task(enviar(websocket))

        # Termina quando o envio acabar (usuário digitou 'sair')
        # ou quando a conexão cair.
        await asyncio.wait(
            [tarefa_receber, tarefa_enviar],
            return_when=asyncio.FIRST_COMPLETED,
        )

        for tarefa in (tarefa_receber, tarefa_enviar):
            tarefa.cancel()

    print(f"[MURAL] {nome} saiu do mural.")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print()
        print(f"[MURAL] {nome} saiu do mural.")
