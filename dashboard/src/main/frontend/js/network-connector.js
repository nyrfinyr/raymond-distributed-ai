import { Network } from "vis-network/peer/esm/vis-network";
import { DataSet } from "vis-data/peer/esm/vis-data";

window.initNetworkGraph = (element) => {
    // 1. Inizializzazione DataSet vuoti
    // I nodi e gli archi li aggiungeremo da Java subito dopo l'init
    const nodes = new DataSet([]);
    const edges = new DataSet([]);

    // 2. Configurazione Vis.js per l'algoritmo di Raymond
    const options = {
        nodes: {
            shape: 'dot',
            size: 30,
            font: { size: 16, color: '#000000' },
            borderWidth: 2,
            shadow: true
        },
        edges: {
            width: 2,
            color: { color: '#848484', highlight: '#848484'},
            arrows: 'to', // FONDAMENTALE: Mostra la direzione dell'holder
            smooth: { type: 'continuous' }
        },
        physics: {
            enabled: true,
            solver: 'forceAtlas2Based', // Buon layout per alberi
            stabilization: { iterations: 150 }
        },
        interaction: { dragNodes: true, zoomView: true }
    };

    // 3. Creazione del Network dentro il <div> (element) passato da Java
    const network = new Network(element, { nodes, edges }, options);

    // --- FUNZIONI ESPOSTE A JAVA ---
    // Le attacchiamo direttamente all'elemento DOM gestito da Vaadin

    // A. Caricamento iniziale o reset totale
    element.setGraphData = (nodesJson, edgesJson) => {
        nodes.clear();
        edges.clear();
        nodes.add(JSON.parse(nodesJson));
        edges.add(JSON.parse(edgesJson));
        network.fit(); // Centra il grafo
    };

    // B. Aggiorna stato/colore nodo (Idle, Requesting, Critical)
    element.updateNodeColor = (nodeId, colorCode) => {
        try {
            nodes.update({ id: nodeId, color: { background: colorCode } });
        } catch (e) { console.error("Errore update nodo", e); }
    };

    // C. Inverte la direzione dell'arco (Raymond logic)
    // "from" ora punta a "to" come suo holder.
    element.updateEdgeDirection = (fromId, toId) => {
        // Rimuovi arco esistente tra i due (indipendentemente dalla direzione)
        const items = edges.get({
            filter: function (item) {
                return (item.from === fromId && item.to === toId) ||
                    (item.from === toId && item.to === fromId);
            }
        });

        if (items.length > 0) {
            edges.remove(items[0].id);
            // Aggiungi nuovo arco orientato correttamente
            edges.add({ from: fromId, to: toId });
        }
    };

    // D. Centra la vista per includere tutti i nodi
    element.fitGraph = () => {
        network.fit({
            animation: {
                duration: 1000,
                easingFunction: 'easeInOutQuad'
            }
        });
    };
};