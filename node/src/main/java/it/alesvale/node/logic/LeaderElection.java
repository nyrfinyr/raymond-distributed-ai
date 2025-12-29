package it.alesvale.node.logic;

import io.nats.client.Dispatcher;
import io.nats.client.Message;
import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;

/**
 * Gestisce il processo di elezione del leader all'interno del cluster utilizzando un approccio
 * basato su protocollo Gossip.
 * <p>
 * L'algoritmo implementa una strategia "Min-ID wins" (vince l'ID più piccolo) propagata tramite messaggi asincroni.
 * Il funzionamento è il seguente:
 * <ol>
 *     <li>Ogni nodo inizia assumendo che il leader sia quello attualmente noto nel suo {@link NodeState}.</li>
 *     <li>All'avvio (`run`), il nodo pubblica l'ID del leader che conosce sul canale "leader-election".</li>
 *     <li>Quando viene ricevuto un messaggio da un altro nodo (`handleLeaderEvent`):
 *         <ul>
 *             <li>Viene confrontato l'ID ricevuto con l'ID del leader attuale locale.</li>
 *             <li>Se l'ID ricevuto è <strong>minore</strong> (più piccolo) di quello attuale, significa che esiste
 *                 un candidato migliore. Il nodo aggiorna il suo stato e "spettegola" (gossip)
 *                 immediatamente la nuova informazione ri-pubblicandola.</li>
 *             <li>Se l'ID ricevuto è maggiore o uguale, il messaggio viene ignorato (convergenza raggiunta o informazione obsoleta).</li>
 *         </ul>
 *     </li>
 * </ol>
 * Questo meccanismo assicura che, eventualmente, l'ID più piccolo del cluster si diffonda a tutti i nodi.
 */
public class LeaderElection {

    Broker broker;
    NodeState nodeState;

    /**
     * Inizializza il processo di elezione.
     *
     * @param nodeState Lo stato corrente del nodo, contenente l'informazione su chi è il leader attuale.
     */
    public LeaderElection(NodeState nodeState, Broker broker){
        this.broker = broker;
        this.nodeState = nodeState;
    }

    /**
     * Avvia il protocollo di elezione.
     * <p>
     * Sottoscrive il nodo al topic "leader-election" e invia il primo messaggio di gossip
     * contenente l'ID del leader attualmente noto.
     */
    public void start() throws InterruptedException {

        Dispatcher d = broker.createDispatcher(msg -> handleLeaderEvent(msg, this.nodeState));

        d.subscribe("leader-election");

        Thread.sleep(1000);
        broker.publishId(nodeState.getLeaderId().nodeId(), "leader-election");
    }

    /**
     * Gestisce la ricezione di un evento di elezione (messaggio di gossip).
     * <p>
     * Implementa la logica di aggiornamento e propagazione: se il senderId ricevuto è migliore (minore)
     * del leader attuale, aggiorna lo stato locale e propaga la notizia alla rete.
     *
     * @param message   Il messaggio ricevuto dal broker contenente l'ID del leader proposto.
     * @param nodeState Lo stato del nodo da aggiornare in caso di nuovo leader.
     */
    private void handleLeaderEvent(Message message, NodeState nodeState){
        Dto.NodeId senderId = new Dto.NodeId(new String(message.getData()));

        if(senderId.compareTo(nodeState.getLeaderId()) < 0){
            nodeState.setLeaderId(senderId);
            broker.publishId(nodeState.getLeaderId().nodeId(), "leader-election");
        }

    }
}
