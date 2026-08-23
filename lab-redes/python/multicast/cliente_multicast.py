import socket
import struct
import sys

# OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
OFFSET = 81

GRUPO = "230.0.0.1"
PORTA = 4446 + OFFSET

# Nome opcional só para identificar o cliente nos logs:
#   python3 cliente_multicast.py Aluno-1
nome = sys.argv[1] if len(sys.argv) > 1 else "Aluno"

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP) as cliente:
    cliente.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    # ADAPTACAO PARA macOS
    # No macOS (e nos BSD em geral) o SO_REUSEADDR sozinho nao permite que dois
    # processos escutem a mesma porta: o segundo cliente falha com
    # "OSError: [Errno 48] Address already in use". O SO_REUSEPORT resolve.
    # O hasattr existe porque essa opcao nao existe no Windows.
    if hasattr(socket, "SO_REUSEPORT"):
        cliente.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)

    cliente.bind(("", PORTA))

    # Entra no grupo multicast. O formato "=4sl" produz exatamente os 8 bytes
    # da struct ip_mreq (endereço do grupo + interface). Sem o "=", o Python
    # usa alinhamento nativo e gera 16 bytes, o que pode ser recusado no macOS.
    mreq = struct.pack("=4sl", socket.inet_aton(GRUPO), socket.INADDR_ANY)
    cliente.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

    print(f"[MULTICAST] {nome} entrou no grupo {GRUPO}:{PORTA}")
    print("[MULTICAST] Aguardando avisos... (Ctrl+C para sair)")
    print()

    try:
        while True:
            dados, endereco = cliente.recvfrom(1024)
            print(f"[{nome}] recebeu -> {dados.decode('utf-8')}")
    except KeyboardInterrupt:
        print()
        print(f"[MULTICAST] {nome} saiu do grupo.")
