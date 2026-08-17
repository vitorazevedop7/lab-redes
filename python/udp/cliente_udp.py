import socket

# OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
OFFSET = 81

HOST = "localhost"
PORTA = 5001 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as cliente:
    # Sem timeout, o recvfrom() ficaria travado para sempre quando não há
    # servidor. Com 3s, o programa avisa e continua — o que deixa visível
    # que o ENVIO funcionou mesmo sem ninguém do outro lado.
    cliente.settimeout(3.0)

    print(f"[UDP] Cliente pronto. Enviando para {HOST}:{PORTA}.")
    print("[UDP] Digite 'sair' para encerrar.")
    print("[UDP] Não há conexão: cada mensagem é um datagrama independente.")

    while True:
        mensagem = input("> ")

        cliente.sendto(mensagem.encode("utf-8"), (HOST, PORTA))
        print("[UDP] Datagrama enviado (o sendto() não garante entrega).")

        try:
            dados, _ = cliente.recvfrom(1024)
            print(dados.decode("utf-8"))
        except socket.timeout:
            print("[UDP] Nenhuma resposta em 3s. "
                  "O datagrama pode ter se perdido ou não há servidor escutando.")

        if mensagem.lower() == "sair":
            break
