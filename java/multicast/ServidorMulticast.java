import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServidorMulticast {

    // OFFSET pessoal (secao 3.3 do roteiro): dois ultimos digitos da matricula 892281
    static final int OFFSET = 81;

    static final String GRUPO = "230.0.0.1";

    public static void main(String[] args) throws Exception {
        int porta = 4446 + OFFSET;
        InetAddress grupo = InetAddress.getByName(GRUPO);

        // O servidor NAO precisa entrar no grupo para enviar - so para receber.
        // Um DatagramSocket comum basta para publicar no endereco de grupo.
        try (MulticastSocket socket = new MulticastSocket()) {

            // TTL controla quantos roteadores o pacote pode atravessar.
            // 1 = nao sai da rede local. E o padrao seguro para laboratorio.
            socket.setTimeToLive(1);

            System.out.println("[MULTICAST] Servidor publicando em " + GRUPO + ":" + porta);
            System.out.println("[MULTICAST] TTL = " + socket.getTimeToLive()
                    + " (o pacote nao atravessa roteadores)");
            System.out.println();

            String[] avisos = {
                "Aviso 1: a prova sera na proxima quarta-feira.",
                "Aviso 2: o laboratorio estara aberto ate as 22h.",
                "Aviso 3: entrega do trabalho foi adiada em uma semana.",
                "Aviso 4: nao havera aula na sexta-feira.",
                "Aviso 5: monitoria extra no sabado de manha."
            };

            for (int i = 0; i < avisos.length; i++) {
                byte[] dados = avisos[i].getBytes(StandardCharsets.UTF_8);
                DatagramPacket pacote = new DatagramPacket(dados, dados.length, grupo, porta);
                socket.send(pacote);
                System.out.println("[MULTICAST] Enviado -> " + avisos[i]);

                // Pausa para dar tempo de derrubar um cliente no meio do envio
                // (necessario para responder a pergunta 3 do roteiro).
                Thread.sleep(3000);
            }

            System.out.println();
            System.out.println("[MULTICAST] Todos os avisos foram publicados.");
        }
    }
}
