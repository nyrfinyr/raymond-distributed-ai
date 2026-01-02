package it.alesvale.node.service;

import it.alesvale.node.data.Dto;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Slf4j
public class SocketManager {

    private final ServerSocket serverSocket;

    @Setter
    private Consumer<Dto.RaymondEvent> onReceive;

    public SocketManager(){
        this.onReceive = null;
        this.serverSocket = initializeServerSocket();
        Thread connectionAcceptThread = createAcceptThread(this.serverSocket);
        connectionAcceptThread.start();
    }

    public Dto.SocketAddress getSocketAddress() throws UnknownHostException {
        String discoveryHost = InetAddress.getLocalHost().getHostAddress();
        int localPort = serverSocket.getLocalPort();
        return new Dto.SocketAddress(discoveryHost, localPort);
    }

    public void send(Dto.RaymondEvent data, Dto.SocketAddress socketAddress) {
        String targetHost = socketAddress.hostname();
        int targetPort = socketAddress.port();

        new Thread(() -> {
            try (Socket socket = new Socket(targetHost, targetPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                log.debug("Sending data to {}:{}", targetHost, targetPort);
                out.writeObject(data);
                out.flush();
            } catch (IOException e) {
                log.error("Error during send", e);
                throw new RuntimeException(e);
            }
        }).start();
    }

    /**
     * Tenta di aprire una ServerSocket su una porta casuale.
     * Se fallisce, riprova finché non ne trova una libera.
     */
    private ServerSocket initializeServerSocket() {
        ServerSocket socket = null;
        while (socket == null) {
            int MIN_PORT = 20000;
            int MAX_PORT = 60000;
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
     */
    private Thread createAcceptThread(ServerSocket serverSocket) {
        return new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    handleIncomingConnection(clientSocket);
                }
            } catch (IOException e) {
                log.error("Errore nel thread di ascolto socket", e);
            }
        });
    }

    /**
     * Accetta connessioni e legge un Object serializzato in arrivo.
     */
    private void handleIncomingConnection(Socket clientSocket) {
        new Thread(() -> {
            try (clientSocket;
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                log.debug("Connection received from: {}", clientSocket.getInetAddress());
                Dto.RaymondEvent receivedData = (Dto.RaymondEvent) in.readObject();
                if(this.onReceive == null){
                    log.warn("OnReceive method not set");
                    return;
                }
                this.onReceive.accept(receivedData);
            } catch (IOException | ClassNotFoundException e) {
                log.error("Errore durante la lettura del messaggio in ingresso", e);
            }
        }).start();
    }
}
