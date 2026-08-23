import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClienteUDP {

    // OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
    static final int OFFSET = 81;

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int porta = 5001 + OFFSET;

        try (DatagramSocket cliente = new DatagramSocket();
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            // Sem timeout, o receive() ficaria travado para sempre quando nao ha
            // servidor. Com 3s, o programa avisa e continua - o que deixa visivel
            // que o ENVIO funcionou mesmo sem ninguem do outro lado.
            cliente.setSoTimeout(3000);

            InetAddress enderecoServidor = InetAddress.getByName(host);

            System.out.println("[UDP] Cliente pronto. Enviando para " + host + ":" + porta + ".");
            System.out.println("[UDP] Digite 'sair' para encerrar.");
            System.out.println("[UDP] Nao ha conexao: cada mensagem e um datagrama independente.");

            String linha;
            while (true) {
                System.out.print("> ");
                linha = teclado.readLine();
                if (linha == null) break;

                byte[] dados = linha.getBytes(StandardCharsets.UTF_8);
                cliente.send(new DatagramPacket(dados, dados.length, enderecoServidor, porta));
                System.out.println("[UDP] Datagrama enviado (o send() nao garante entrega).");

                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
                    cliente.receive(resposta);
                    System.out.println(new String(resposta.getData(), 0,
                            resposta.getLength(), StandardCharsets.UTF_8));
                } catch (SocketTimeoutException e) {
                    System.out.println("[UDP] Nenhuma resposta em 3s. "
                            + "O datagrama pode ter se perdido ou nao ha servidor escutando.");
                }

                if (linha.equalsIgnoreCase("sair")) break;
            }
        }
    }
}
