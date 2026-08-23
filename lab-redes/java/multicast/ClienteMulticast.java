import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class ClienteMulticast {

    // OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
    static final int OFFSET = 81;

    static final String GRUPO = "230.0.0.1";

    public static void main(String[] args) throws Exception {
        int porta = 4446 + OFFSET;
        InetAddress grupo = InetAddress.getByName(GRUPO);

        // Nome opcional so para identificar o cliente nos logs: java ClienteMulticast Aluno-1
        String nome = args.length > 0 ? args[0] : "Aluno";

        // ADAPTACAO PARA macOS
        // O roteiro usa NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
        // que no macOS costuma devolver null (o hostname nem sempre resolve para uma
        // interface real) e faz o joinGroup lancar excecao. Aqui a interface e
        // escolhida testando as que estao ativas e suportam multicast.
        NetworkInterface interfaceRede = escolherInterface();

        if (interfaceRede == null) {
            System.err.println("ERRO: nenhuma interface de rede com suporte a multicast foi encontrada.");
            System.err.println("Verifique se o Wi-Fi esta ligado e tente novamente.");
            listarInterfaces();
            return;
        }

        try (MulticastSocket socket = new MulticastSocket(porta)) {
            socket.setReuseAddress(true);

            SocketAddress enderecoGrupo = new InetSocketAddress(grupo, porta);
            socket.joinGroup(enderecoGrupo, interfaceRede);

            System.out.println("[MULTICAST] " + nome + " entrou no grupo " + GRUPO + ":" + porta);
            System.out.println("[MULTICAST] Interface usada: " + interfaceRede.getName()
                    + " (" + interfaceRede.getDisplayName() + ")");
            System.out.println("[MULTICAST] Aguardando avisos... (Ctrl+C para sair)");
            System.out.println();

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);
                String mensagem = new String(
                        pacote.getData(), 0, pacote.getLength(), StandardCharsets.UTF_8);
                System.out.println("[" + nome + "] recebeu -> " + mensagem);
            }
        }
    }

    /**
     * Devolve a primeira interface ativa que suporta multicast e nao e loopback.
     * Em ultimo caso aceita o loopback, util quando a maquina esta sem rede.
     */
    private static NetworkInterface escolherInterface() throws SocketException {
        NetworkInterface loopback = null;

        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || !ni.supportsMulticast()) continue;
            if (ni.isLoopback()) {
                loopback = ni;
                continue;
            }
            // Precisa ter um endereco IPv4 configurado para valer como candidata
            for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                if (addr.getAddress() instanceof Inet4Address) {
                    return ni;
                }
            }
        }
        return loopback;
    }

    private static void listarInterfaces() throws SocketException {
        System.err.println("--- interfaces detectadas ---");
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            System.err.printf("  %-8s up=%-5s multicast=%-5s loopback=%s%n",
                    ni.getName(), ni.isUp(), ni.supportsMulticast(), ni.isLoopback());
        }
    }
}
