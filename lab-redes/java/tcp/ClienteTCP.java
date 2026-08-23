import java.io.*;
import java.net.*;

public class ClienteTCP {

    // OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
    static final int OFFSET = 81;

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int porta = 5000 + OFFSET;

        try (Socket socket = new Socket(host, porta);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("[TCP] Conectado ao servidor na porta " + porta + ".");
            System.out.println("[TCP] Digite 'hora' para pedir o horario ou 'sair' para encerrar.");

            String linha;
            while (true) {
                System.out.print("> ");
                linha = teclado.readLine();
                if (linha == null) break;

                saida.println(linha);
                System.out.println(entrada.readLine());

                if (linha.equalsIgnoreCase("sair")) break;
            }
        }
    }
}
