# Distributed Mutual Exclusion: Raymond's Tree-Based Algorithm

## 📌 Introduzione
Questo repository ospita il progetto finale per il corso di **Distributed Artificial Intelligence**.
Il progetto consiste nell'implementazione distribuita e nella visualizzazione grafica dell'**Algoritmo di Raymond (1989)** per la mutua esclusione in sistemi distribuiti.

A differenza degli algoritmi classici basati su topologie ad anello (Token Ring) o su grafo completo (Ricart & Agrawala), l'algoritmo di Raymond organizza i nodi in una topologia ad **albero logico**, permettendo di ottenere una complessità media di messaggi pari a $O(\log N)$.

L'applicazione è containerizzata tramite **Docker** e prevede un'interfaccia di visualizzazione dinamica a grafo realizzata in **Vaadin**, che funge da "Osservatore" dello stato globale del sistema.

---

## 🧠 Cenni Teorici: L'Algoritmo di Raymond

L'algoritmo K. Raymond utilizza una struttura ad albero (Spanning Tree) per gestire il passaggio di un **Token** (privilegio) che garantisce l'accesso alla Sezione Critica (CS).

### Concetti Chiave
1.  **Topologia Logica:** I nodi sono organizzati in un albero. Ogni nodo comunica solo con il proprio genitore e i propri figli diretti.
2.  **Holder (Puntatore al Token):** Ogni nodo mantiene una variabile `holder` che punta verso il vicino (genitore o figlio) che si trova lungo il percorso verso il Token (o che possiede il Token stesso). Le frecce del grafo visualizzato rappresentano proprio questa variabile: **tutte le frecce puntano verso la radice corrente (il detentore del Token).**
3.  **Request Queue:** Ogni nodo possiede una coda FIFO locale dove memorizza le richieste di accesso alla CS (proprie o dei figli).

### Funzionamento
Il flusso di una richiesta avviene in due fasi:

1.  **Fase di Richiesta (Salita):**
    * Quando un nodo vuole accedere alla CS, aggiunge se stesso alla propria coda.
    * Se la coda era vuota, invia un messaggio `REQUEST` al suo `holder`.
    * Il messaggio risale l'albero fino a raggiungere il nodo che possiede il Token (la radice).

2.  **Fase di Concessione (Discesa del Token):**
    * Il nodo radice, appena possibile, cede il Token al vicino che ha fatto richiesta.
    * **Riorientamento Dinamico:** Quando il Token passa dal nodo A al nodo B, la variabile `holder` di A viene aggiornata per puntare a B. Di conseguenza, **la topologia dell'albero cambia**: B diventa la nuova radice.
    * Il Token scende lungo il percorso fino al nodo richiedente originale, che entra in Sezione Critica.

---

## 🏗️ Architettura del Sistema

Il sistema è un'applicazione distribuita reale, dove ogni nodo è un processo isolato che non condivide memoria con gli altri.

### Stack Tecnologico
* **Linguaggio:** Java 17+
* **Framework Middleware:** Spring Boot (Web, Actuator)
* **Frontend / Visualizer:** Vaadin Flow + Vis.js (per la visualizzazione a grafo)
* **Containerization:** Docker & Docker Compose
* **Build Tool:** Maven

### Componenti
1.  **Distributed Node (Microservizio):** * Espone API REST per lo scambio di messaggi (`POST /api/request`, `POST /token`).
    * Implementa la logica stateful di Raymond (gestione coda FIFO e puntatori).
    * Esegue un thread separato per simulare il lavoro nella Sezione Critica.
2.  **Observer Dashboard (Vaadin):**
    * Interroga periodicamente i nodi (Polling o Push) per ottenere lo stato corrente (`holder`, `queue`, `status`).
    * Renderizza la topologia dinamica:
        * **Nodi Verdi:** Idle
        * **Nodi Gialli:** In attesa (Request inviata)
        * **Nodi Rossi:** In Sezione Critica (Token posseduto)
    * Mostra gli archi direzionati in base alla variabile `holder`.

---

## 🚀 Guida all'Esecuzione

### Prerequisiti
* Docker & Docker Compose installati sulla macchina.
* Porte 8080-808X libere.

### Avvio Rapido
1.  Clonare il repository:
    ```bash
    git clone [https://github.com/tuo-user/raymond-algorithm-visualizer.git](https://github.com/tuo-user/raymond-algorithm-visualizer.git)
    cd raymond-algorithm-visualizer
    ```

2.  Build del progetto (genera il .jar):
    ```bash
    mvn clean package -Pproduction
    ```

3.  Avvio dell'infrastruttura:
    ```bash
    docker-compose up --build
    ```
    *Questo comando avvierà 5 istanze del nodo e 1 istanza della dashboard.*

4.  Accesso alla Dashboard:
    Aprire il browser all'indirizzo: `http://localhost:8080`

### Utilizzo
1.  Dalla Dashboard, cliccare su un nodo qualsiasi nel grafo per forzare una **Richiesta di Mutua Esclusione**.
2.  Osservare il cambio di colore del nodo (Giallo).
3.  Osservare il movimento logico del Token (i nodi diventano rossi sequenzialmente).
4.  Notare come le **frecce (archi) si invertono** al passaggio del Token, riconfigurando la radice dell'albero.

---