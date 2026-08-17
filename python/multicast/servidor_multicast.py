import socket
import struct
import time

# OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
OFFSET = 81

GRUPO = "230.0.0.1"
PORTA = 4446 + OFFSET

# O servidor não precisa entrar no grupo para enviar, apenas para receber.
with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as servidor:

    # TTL controla quantos roteadores o pacote pode atravessar.
    # 1 = não sai da rede local. É o padrão seguro para laboratório.
    ttl = struct.pack("b", 1)
    servidor.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, ttl)

    print(f"[MULTICAST] Servidor publicando em {GRUPO}:{PORTA}")
    print("[MULTICAST] TTL = 1 (o pacote não atravessa roteadores)")
    print()

    avisos = [
        "Aviso 1: a prova será na próxima quarta-feira.",
        "Aviso 2: o laboratório estará aberto até as 22h.",
        "Aviso 3: entrega do trabalho foi adiada em uma semana.",
        "Aviso 4: não haverá aula na sexta-feira.",
        "Aviso 5: monitoria extra no sábado de manhã.",
    ]

    for aviso in avisos:
        servidor.sendto(aviso.encode("utf-8"), (GRUPO, PORTA))
        print(f"[MULTICAST] Enviado -> {aviso}")

        # Pausa para dar tempo de derrubar um cliente no meio do envio
        # (necessário para responder a pergunta 3 do roteiro).
        time.sleep(3)

    print()
    print("[MULTICAST] Todos os avisos foram publicados.")
