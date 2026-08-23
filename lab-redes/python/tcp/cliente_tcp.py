import socket

# OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
OFFSET = 81

HOST = "localhost"
PORTA = 5000 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as cliente:
    cliente.connect((HOST, PORTA))
    print(f"[TCP] Conectado ao servidor na porta {PORTA}.")
    print("[TCP] Digite 'hora' para pedir o horário ou 'sair' para encerrar.")

    arquivo = cliente.makefile("r")

    while True:
        mensagem = input("> ")
        cliente.sendall((mensagem + "\n").encode("utf-8"))
        print(arquivo.readline().strip())

        if mensagem.lower() == "sair":
            break
