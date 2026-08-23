import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MuralServidor extends WebSocketServer {

    // OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
    static final int OFFSET = 81;
    static final int PORTA = 8887 + OFFSET;

    public MuralServidor(int porta) {
        super(new InetSocketAddress(porta));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Diferente do multicast, aqui o SERVIDOR mantem a lista de conectados.
        // getConnections() e essa lista.
        System.out.println("[MURAL] Novo aluno conectado: " + conn.getRemoteSocketAddress());
        System.out.println("[MURAL] Alunos no mural agora: " + getConnections().size());

        conn.send("Bem-vindo ao mural da turma! Voce esta conectado.");
        difundir("Um novo aluno entrou no mural. Total: " + getConnections().size(), conn);
    }

    @Override
    public void onMessage(WebSocket conn, String mensagem) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[MURAL] Aviso recebido de "
                + conn.getRemoteSocketAddress() + ": " + mensagem);

        // Difunde para TODOS os conectados, inclusive quem enviou.
        String aviso = "[" + hora + "] Aviso da turma: " + mensagem;
        for (WebSocket cliente : getConnections()) {
            cliente.send(aviso);
        }
    }

    @Override
    public void onClose(WebSocket conn, int codigo, String motivo, boolean remoto) {
        System.out.println("[MURAL] Aluno saiu: " + conn.getRemoteSocketAddress()
                + " (codigo " + codigo + ")");
        System.out.println("[MURAL] Alunos no mural agora: " + getConnections().size());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[MURAL] Erro: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[MURAL] Servidor WebSocket ouvindo na porta " + PORTA);
        System.out.println("[MURAL] Aguardando conexoes... (Ctrl+C para encerrar)");
        System.out.println();
    }

    /** Envia para todos menos o remetente. */
    private void difundir(String texto, WebSocket exceto) {
        for (WebSocket cliente : getConnections()) {
            if (cliente != exceto) {
                cliente.send(texto);
            }
        }
    }

    public static void main(String[] args) {
        MuralServidor servidor = new MuralServidor(PORTA);
        servidor.start();
    }
}
