# Raymond Distributed AI

Implementazione dell'algoritmo di **mutua esclusione distribuita di Raymond** con visualizzazione real-time tramite dashboard web.

## Stack Tecnologico

### Backend
| Tecnologia | Versione | Utilizzo |
|------------|----------|----------|
| Java | 21 | Linguaggio principale |
| Spring Boot | 4.0.1 | Framework web (Dashboard) |
| Vaadin | 25.0.0 | UI framework con server-push |
| NATS | Alpine | Message broker pub/sub |
| Jackson | 2.18.2 | Serializzazione JSON |
| Lombok | 1.18.36 | Riduzione boilerplate |
| Gradle | 8.5+ | Build automation |

### Frontend
| Tecnologia | Versione | Utilizzo |
|------------|----------|----------|
| React | 19.x | Component framework |
| TypeScript | 5.9.x | Typed JavaScript |
| Vis.js Network | 9.1.9 | Visualizzazione grafi |
| Vite | 7.3.x | Build tool |

### DevOps
| Tecnologia | Utilizzo |
|------------|----------|
| Docker | Containerizzazione |
| Docker Swarm | Orchestrazione cluster |
| Alpine Linux | Base image leggera |

## Architettura

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Swarm                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────────────────────────────┐     │
│  │    NATS     │◄───│           Dashboard                 │     │
│  │   Broker    │    │  (Spring Boot + Vaadin + Vis.js)    │     │
│  │  :4222      │    │            :8080                    │     │
│  └──────┬──────┘    └─────────────────────────────────────┘     │
│         │                                                       │
│         │ pub/sub                                               │
│         │                                                       │
│  ┌──────┴──────────────────────────────────────────────┐        │
│  │                                                      │       │
│  │  ┌────────┐  ┌────────┐  ┌────────┐    ┌────────┐  │         │
│  │  │ Node 1 │  │ Node 2 │  │ Node 3 │ .. │ Node N │  │         │
│  │  └───┬────┘  └───┬────┘  └───┬────┘    └───┬────┘  │         │
│  │      │           │           │              │       │        │
│  │      └───────────┴─────┬─────┴──────────────┘       │        │
│  │                        │                            │        │
│  │              Socket P2P (TCP)                       │        │
│  │         (Raymond REQUEST/PRIVILEGE)                 │        │
│  └─────────────────────────────────────────────────────┘        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Comunicazione
- **NATS Broker**: Comunicazione pub/sub per eventi di sistema (leader election, spanning tree, dashboard updates)
- **Socket TCP P2P**: Comunicazione diretta tra nodi per messaggi Raymond (REQUEST/PRIVILEGE)
- **WebSocket (Vaadin Push)**: Aggiornamenti real-time dalla dashboard al browser

## Servizi Implementati

Il sistema esegue tre fasi in sequenza, gestite da una **State Machine**:

### 1. Leader Election Service

**Algoritmo**: Gossip-based con strategia "Min-ID wins"

**Funzionamento**:
1. Ogni nodo pubblica il proprio ID sul topic `leader-election`
2. Alla ricezione di un messaggio, confronta l'ID ricevuto con quello del leader corrente
3. Se l'ID ricevuto è minore, aggiorna il leader e propaga l'informazione
4. Stabilizzazione dopo 20 secondi di silenzio

**Topic NATS**: `leader-election`

```java
// Il nodo con ID più piccolo diventa leader
if (proposedLeaderId.compareTo(currentLeaderId) < 0) {
    nodeState.setLeaderId(proposedLeaderId);
    broker.publishId(nodeState.getLeaderId(), "leader-election");
}
```

### 2. Spanning Tree Service

**Algoritmo**: Costruzione incrementale con adozione parent-child

**Funzionamento**:
1. Il leader inizia a fare broadcast della propria identità
2. I nodi orfani ascoltano sul topic `spanning-tree.announce` (con queue group)
3. Quando un orfano riceve un annuncio, adotta il mittente come parent
4. Il nuovo nodo inizia a sua volta il broadcast per adottare altri orfani
5. Stabilizzazione dopo 20 secondi senza nuovi join

**Topic NATS**:
- `spanning-tree.announce` - Annunci per adozione
- `spanning-tree.joined` - Notifica di join (reset timer stabilizzazione)
- `spanning-tree.stabilized` - Broadcast fine costruzione

### 3. Raymond Mutual Exclusion Service

**Algoritmo**: Raymond's Tree-Based Algorithm (token-based)

**Concetti chiave**:
- **HOLDER**: Puntatore al nodo che detiene (o conosce la direzione del) privilegio
- **USING**: Flag che indica se il nodo sta usando la sezione critica
- **REQUEST_Q**: Coda delle richieste pendenti
- **ASKED**: Flag per evitare richieste duplicate

**Procedure principali**:

```java
ASSIGN_PRIVILEGE():
    // Precondizioni: holder && !using && !queue.isEmpty
    // Passa il privilegio al primo della coda

MAKE_REQUEST():
    // Precondizioni: !holder && !queue.isEmpty && !asked
    // Invia REQUEST verso il holder
```

**Eventi gestiti**:
- `REQUEST`: Ricezione richiesta da un vicino -> accoda e chiama ASSIGN_PRIVILEGE + MAKE_REQUEST
- `PRIVILEGE`: Ricezione del token -> diventa holder e chiama ASSIGN_PRIVILEGE + MAKE_REQUEST

**Comunicazione**: Socket TCP diretti tra nodi (non NATS)

### Ciclo di vita del nodo

```
┌──────────────────┐     stabilized     ┌─────────────────────┐
│  LEADER_ELECTION │ ─────────────────► │ BUILDING_SPANNING   │
│                  │                    │       TREE          │
└──────────────────┘                    └──────────┬──────────┘
                                                   │
                                        stabilized │
                                                   ▼
                                        ┌─────────────────────┐
                                        │ RAYMOND_MUTUAL      │
                                        │    EXCLUSION        │
                                        └─────────────────────┘
```

## Dashboard

La dashboard visualizza in real-time:
- **Grafo della rete**: Nodi e connessioni dello spanning tree
- **Stato dei nodi**: IDLE (grigio), REQUESTING (giallo), CRITICAL (verde)
- **Holder del privilegio**: Evidenziato visivamente
- **Log eventi**: Panel laterale con filtri per tipo evento

**Tecnologie**:
- Vaadin con `@Push` per aggiornamenti WebSocket
- Vis.js Network per rendering del grafo con fisica
- Sottoscrizione NATS per ricevere eventi dai nodi

## Avvio con Docker Swarm

### Prerequisiti
- Docker Engine con Swarm mode
- Docker Compose v2

### Comandi

```bash
# 1. Inizializza Docker Swarm (se non già fatto)
docker swarm init

# 2. Build delle immagini e deploy dello stack
./swarm-launch.sh
```

Lo script `swarm-launch.sh` esegue:
```bash
docker compose build                    # Build immagini
docker stack rm raymond                 # Rimuove stack precedente
docker stack up -c compose.yaml raymond # Deploy nuovo stack
docker service logs -f raymond_node     # Follow dei log
```

### Configurazione Stack

Il file `compose.yaml` definisce:

| Servizio | Immagine | Repliche | Porte |
|----------|----------|----------|-------|
| broker | nats:alpine | 1 | 8222 (monitoring) |
| dashboard | dashboard:latest | 1 | 8091 -> 8080 |
| node | node-worker:latest | 10 | - |

### Accesso alla Dashboard

Una volta avviato lo stack:
```
http://localhost:8091
```

### Scaling dei nodi

```bash
# Aumenta il numero di nodi worker
docker service scale raymond_node=20
```

### Monitoraggio

```bash
# Stato dei servizi
docker service ls

# Log di un servizio specifico
docker service logs -f raymond_node
docker service logs -f raymond_dashboard

# NATS monitoring
curl http://localhost:8222/varz
```

### Stop dello stack

```bash
docker stack rm raymond
```

## Struttura del Progetto

```
raymond-distributed-ai/
├── node/                          # Modulo nodo distribuito
│   ├── src/main/java/
│   │   └── it/alesvale/node/
│   │       ├── NodeApplication.java
│   │       ├── broker/
│   │       │   └── Broker.java
│   │       ├── data/
│   │       │   ├── Dto.java
│   │       │   ├── NodeState.java
│   │       │   ├── RaymondState.java
│   │       │   └── StateMachine.java
│   │       └── service/
│   │           ├── LeaderElectionService.java
│   │           ├── SpanningTreeService.java
│   │           ├── RaymondService.java
│   │           └── SocketManager.java
│   ├── build.gradle
│   └── Dockerfile
│
├── dashboard/                     # Modulo dashboard web
│   ├── src/main/java/
│   │   └── it/alesvale/dashboard/
│   │       ├── DashboardApplication.java
│   │       ├── backend/
│   │       │   └── BrokerSubscriber.java
│   │       ├── view/
│   │       │   └── MainView.java
│   │       └── component/
│   │           ├── LogSidePanel.java
│   │           └── StatusLegend.java
│   ├── src/main/frontend/
│   │   └── js/
│   │       └── network-connector.js
│   ├── build.gradle
│   └── Dockerfile
│
├── compose.yaml                   # Docker Compose/Swarm config
├── swarm-launch.sh               # Script di avvio
└── README.md
```

## Riferimenti

- Raymond, K. (1989). "A Tree-Based Algorithm for Distributed Mutual Exclusion"
- NATS Documentation: https://docs.nats.io/
- Vaadin Documentation: https://vaadin.com/docs
- Vis.js Network: https://visjs.github.io/vis-network/docs/network/
