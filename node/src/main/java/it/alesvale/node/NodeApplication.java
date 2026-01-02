package it.alesvale.node;

import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import it.alesvale.node.logic.LeaderElection;
import it.alesvale.node.logic.SpanningTree;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NodeApplication {

    private static final int MIN_PORT = 20000;
    private static final int MAX_PORT = 60000;

    public static void main(String[] args) {

        String discoveryHost = System.getenv().getOrDefault("SWARM_SERVICE_NAME", "localhost");
        ServerSocket serverSocket = initializeServerSocket();
        Broker broker = new Broker();

        String swarmName = String.format("%s:%s", discoveryHost, serverSocket.getLocalPort());

        NodeState state = new NodeState(swarmName);
        log.info("[{}] Swarm name: {}", state.getId().getHumanReadableId(), swarmName);

        SpanningTree spanningTree = new SpanningTree(broker, state);
        LeaderElection leaderElection = new LeaderElection(state, broker)
                .then(spanningTree::start);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        scheduler.scheduleAtFixedRate(() -> sendAliveEvent(broker, state), 0, 3, TimeUnit.SECONDS);

        leaderElection.start();
    }

    /**
     * Tenta di aprire una ServerSocket su una porta casuale.
     * Se fallisce, riprova finché non ne trova una libera.
     */
    private static ServerSocket initializeServerSocket() {
        ServerSocket socket = null;
        while (socket == null) {
            int randomPort = ThreadLocalRandom.current().nextInt(MIN_PORT, MAX_PORT);
            try {
                socket = new ServerSocket(randomPort);
                log.info("Socket Server avviato correttamente sulla porta: {}", randomPort);
            } catch (IOException e) {
                log.warn("Porta {} occupata o non disponibile. Riprovo...", randomPort);
            }
        }
        return socket;
    }

    /**
     * Avvia un thread separato per accettare le connessioni in ingresso
     * senza bloccare il main thread.
     */
    private static void startAcceptThread(ServerSocket serverSocket) {
        Thread acceptThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    handleIncomingConnection(clientSocket);
                }
            } catch (IOException e) {
                log.error("Errore nel thread di ascolto socket", e);
            }
        });
        acceptThread.setName("Socket-Acceptor");
        acceptThread.start();
    }

    private static void handleIncomingConnection(Socket clientSocket) {
        new Thread(() -> {
            try {
                log.info("Nuova connessione accettata da: {}", clientSocket.getInetAddress());
                clientSocket.close();
            } catch (IOException e) {
                log.error("Errore gestione client", e);
            }
        }).start();
    }

    private static void sendAliveEvent(Broker broker, NodeState state){
        try {
            Dto.NodeEvent aliveEvent = Dto.NodeEvent.builder()
                    .nodeId(state.getId())
                    .eventType(Dto.NodeEventType.I_AM_ALIVE)
                    .status(state.getStatus())
                    .edgeTo(state.getParent())
                    .leader(state.isLeader())
                    .build();
            broker.publishEvent(aliveEvent);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

}
