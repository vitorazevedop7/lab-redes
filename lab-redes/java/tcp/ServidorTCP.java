import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServidorTCP {

    // OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
    static final int OFFSET = 81;

    public static void main(String[] args) throws IOException {
        int porta = 5000 + OFFSET;

        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("[TCP] Servidor aguardando conexoes na porta " + porta + "...");

            try (Socket cliente = servidor.accept();
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(cliente.getInputStream()));
                 PrintWriter saida = new PrintWriter(cliente.getOutputStream(), true)) {

                System.out.println("[TCP] Cliente conectado: " + cliente.getRemoteSocketAddress());

                String mensagem;
                while ((mensagem = entrada.readLine()) != null) {
                    System.out.println("[TCP] Recebido: " + mensagem);

                    if (mensagem.equalsIgnoreCase("sair")) {
                        saida.println("Encerrando conexao. Ate mais!");
                        break;
                    }

                    // Tarefa 4.5.3: ao receber "hora", responder com o horario do servidor
                    if (mensagem.equalsIgnoreCase("hora")) {
                        String agora = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                        saida.println("Monitor responde: o horario do servidor e " + agora);
                        continue;
                    }

                    saida.println("Monitor responde: recebi sua mensagem -> \"" + mensagem + "\"");
                }
            }
        }
        System.out.println("[TCP] Servidor encerrado.");
    }
}
