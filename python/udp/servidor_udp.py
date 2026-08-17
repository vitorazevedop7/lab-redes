import socket

# OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
OFFSET = 81

HOST = "0.0.0.0"
PORTA = 5001 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as servidor:
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind((HOST, PORTA))
    print(f"[UDP] Servidor aguardando datagramas na porta {PORTA}...")

    while True:
        # Não existe accept(): o servidor apenas espera um datagrama chegar.
        # O endereço do remetente vem junto com os dados, não de uma conexão.
        dados, endereco = servidor.recvfrom(1024)
        mensagem = dados.decode("utf-8").strip()

        print(f"[UDP] Recebido de {endereco[0]}:{endereco[1]} -> {mensagem}")

        if mensagem.lower() == "sair":
            servidor.sendto("Encerrando. Até mais!".encode("utf-8"), endereco)
            break

        resposta = f'Monitor responde: recebi sua mensagem -> "{mensagem}"'
        servidor.sendto(resposta.encode("utf-8"), endereco)

print("[UDP] Servidor encerrado.")
