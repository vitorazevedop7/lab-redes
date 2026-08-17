import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class MuralCliente {

    // OFFSET pessoal (secao 3.3 do roteiro) - deve ser o MESMO do servidor
    static final int OFFSET = 81;
    static final int PORTA = 8887 + OFFSET;

    public static void main(String[] args) throws Exception {
        String nome = args.length > 0 ? args[0] : "Aluno";
        String url = "ws://localhost:" + PORTA;

        // Este cliente NAO precisa da biblioteca externa: java.net.http.WebSocket
        // faz parte do JDK desde a versao 11.
        HttpClient http = HttpClient.newHttpClient();

        WebSocket ws = http.newWebSocketBuilder()
                .buildAsync(URI.create(url), new Ouvinte(nome))
                .join();

        System.out.println("[MURAL] " + nome + " conectado em " + url);
        System.out.println("[MURAL] Digite um aviso e tecle Enter. 'sair' encerra.");
        System.out.println();

        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            String linha;
            while ((linha = teclado.readLine()) != null) {
                if (linha.equalsIgnoreCase("sair")) break;
                ws.sendText(nome + " diz: " + linha, true);
            }
        }

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "cliente encerrou").join();
        System.out.println("[MURAL] " + nome + " saiu do mural.");
    }

    /** Recebe as mensagens difundidas pelo servidor. */
    static class Ouvinte implements WebSocket.Listener {

        private final String nome;

        Ouvinte(String nome) {
            this.nome = nome;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            // Sem o request(1) o cliente nao pede a proxima mensagem e nada chega.
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence dados, boolean fim) {
            System.out.println("  >> " + dados);
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable erro) {
            System.err.println("[MURAL] Erro no cliente " + nome + ": " + erro.getMessage());
        }
    }
}
