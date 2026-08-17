import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServidorUDP {

    // OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
    static final int OFFSET = 81;

    public static void main(String[] args) throws Exception {
        int porta = 5001 + OFFSET;

        try (DatagramSocket servidor = new DatagramSocket(porta)) {
            System.out.println("[UDP] Servidor aguardando datagramas na porta " + porta + "...");

            byte[] buffer = new byte[1024];

            while (true) {
                // Nao existe accept(): o servidor apenas espera um datagrama chegar.
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                servidor.receive(pacote);

                String mensagem = new String(
                        pacote.getData(), 0, pacote.getLength(), StandardCharsets.UTF_8).trim();

                // O endereco do remetente vem dentro do proprio pacote, nao de uma conexao.
                InetAddress enderecoCliente = pacote.getAddress();
                int portaCliente = pacote.getPort();

                System.out.println("[UDP] Recebido de " + enderecoCliente.getHostAddress()
                        + ":" + portaCliente + " -> " + mensagem);

                if (mensagem.equalsIgnoreCase("sair")) {
                    String despedida = "Encerrando. Ate mais!";
                    byte[] dados = despedida.getBytes(StandardCharsets.UTF_8);
                    servidor.send(new DatagramPacket(dados, dados.length,
                            enderecoCliente, portaCliente));
                    break;
                }

                String resposta = "Monitor responde: recebi sua mensagem -> \"" + mensagem + "\"";
                byte[] dados = resposta.getBytes(StandardCharsets.UTF_8);
                servidor.send(new DatagramPacket(dados, dados.length,
                        enderecoCliente, portaCliente));
            }
        }
        System.out.println("[UDP] Servidor encerrado.");
    }
}
