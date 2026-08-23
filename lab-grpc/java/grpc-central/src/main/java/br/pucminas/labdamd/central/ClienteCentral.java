package br.pucminas.labdamd.central;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class ClienteCentral {

    // OFFSET pessoal - deve ser o MESMO do servidor
    static final int OFFSET = 81;

    public static void main(String[] args) {
        int porta = 50051 + OFFSET;

        ManagedChannel canal = ManagedChannelBuilder.forAddress("localhost", porta)
                .usePlaintext()
                .build();

        try {
            CentralAtendimentoGrpc.CentralAtendimentoBlockingStub stub =
                    CentralAtendimentoGrpc.newBlockingStub(canal);

            Scanner teclado = new Scanner(System.in);
            System.out.print("Digite seu nome: ");
            String nome = teclado.nextLine();

            // Chamada unária: parece uma chamada de método local, mas atravessa a rede
            PerguntaHorario pergunta = PerguntaHorario.newBuilder().setNomeAluno(nome).build();
            RespostaHorario resposta = stub.consultarHorario(pergunta);
            System.out.println("[gRPC] " + resposta.getMensagem());
        } finally {
            canal.shutdown();
        }
    }
}
