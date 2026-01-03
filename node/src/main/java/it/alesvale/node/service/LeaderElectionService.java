package it.alesvale.node.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import it.alesvale.node.Utils;
import it.alesvale.node.broker.Broker;
import it.alesvale.node.data.Dto;
import it.alesvale.node.data.NodeState;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
@Slf4j
public class LeaderElectionService implements AgentService{

    Broker broker;
    NodeState nodeState;
    ObjectMapper mapper;
    Integer DEBOUNCE_TIME_S = 20;

    private final ScheduledExecutorService debounceScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> silenceTask;
    private final AtomicBoolean isStable = new AtomicBoolean(false);
    @Setter
    private Runnable onStabilizedCallback;

    public LeaderElectionService(NodeState nodeState, Broker broker){
        this.broker = broker;
        this.nodeState = nodeState;
        this.mapper = Utils.getMapper();
    }

    /**
     * Avvia il protocollo di elezione.
     * <p>
     * Sottoscrive il nodo al topic "leader-election" e invia il primo messaggio di gossip
     * contenente l'ID del leader attualmente noto.
     */
    public void start() {

        log.info("[{}] Starting leader election", nodeState.getHumanReadableId());
        broker.publishInfoMessage(nodeState.getId(), "Starting leader election");

        Dispatcher d = broker.createDispatcher(msg -> handleLeaderEvent(msg, this.nodeState));
        d.subscribe("leader-election");
        try {
            broker.publishId(nodeState.getLeaderId(), "leader-election");
        }catch(JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Gestisce la ricezione di un evento di elezione (messaggio di gossip).
     */
    private void handleLeaderEvent(Message message, NodeState nodeState){
        rescheduleStabilityCheck();

        try {
            Dto.NodeId proposedLeaderId = mapper.readValue(message.getData(), Dto.NodeId.class);
            Dto.NodeId currentLeaderId = nodeState.getLeaderId();

            if (proposedLeaderId.compareTo(currentLeaderId) < 0) {
                nodeState.setLeaderId(proposedLeaderId);
                broker.publishId(nodeState.getLeaderId(), "leader-election");
            }
            else if (proposedLeaderId.compareTo(currentLeaderId) > 0) {
                broker.publishId(currentLeaderId, "leader-election");
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private synchronized void rescheduleStabilityCheck() {
        if (silenceTask != null && !silenceTask.isDone()) {
            silenceTask.cancel(false);
        }

        silenceTask = debounceScheduler.schedule(() -> {
            log.info("[{}] Leader Election stabilized ({}s silence). Leader is {}",
                    nodeState.getHumanReadableId(), DEBOUNCE_TIME_S, nodeState.getLeaderId().getHumanReadableId());
            broker.publishInfoMessage(nodeState.getId(), String.format("Leader Election stabilized: Leader is %s", nodeState.getLeaderId().getHumanReadableId()));
            
            if (onStabilizedCallback != null && isStable.compareAndSet(false, true)) {
                onStabilizedCallback.run();
            }
        }, DEBOUNCE_TIME_S, TimeUnit.SECONDS);
    }
}
