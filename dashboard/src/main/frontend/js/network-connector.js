import { Network } from "vis-network/peer/esm/vis-network";
import { DataSet } from "vis-data/peer/esm/vis-data";

window.initNetworkGraph = (element) => {
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
            arrows: 'to',
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

    // Helper per convertire il DTO Java (nested) nel formato piatto di Vis.js
    // Java NodeData: { nodeId: { nodeId: "1" }, label: "A" } -> Vis: { id: "1", label: "A" }
    const mapNode = (rawNode) => {
        return {
            ...rawNode,
            id: rawNode.nodeId.nodeId, // Mappa nodeId.nodeId -> id
            label: rawNode.label
        };
    };

    // Helper per gli Edge
    // Java EdgeData: { from: { nodeId: "1" }, to: { nodeId: "2" } } -> Vis: { from: "1", to: "2" }
    const mapEdge = (rawEdge) => {
        return {
            ...rawEdge,
            from: rawEdge.from.nodeId, // Estrai la stringa dall'oggetto
            to: rawEdge.to.nodeId      // Estrai la stringa dall'oggetto
        };
    };

    element.setGraphData = (nodesJson, edgesJson) => {
        nodes.clear();
        edges.clear();

        const rawNodes = JSON.parse(nodesJson);
        const rawEdges = JSON.parse(edgesJson);

        nodes.add(rawNodes.map(mapNode));
        edges.add(rawEdges.map(mapEdge));

        network.fit();
    };

    element.addNode = (nodeJson) => {
        const rawNode = JSON.parse(nodeJson);
        const newNode = mapNode(rawNode);

        const existingNode = nodes.get(newNode.id);

        if (existingNode) {
            nodes.update(newNode);
        } else {
            nodes.add(newNode);
        }
        //network.fit();
    }

    element.addEdge = (edgeJson) => {
        const rawEdge = JSON.parse(edgeJson);
        const newEdge = mapEdge(rawEdge);

        const existingEdges = edges.get({
            filter: function (item) {
                return (item.from === newEdge.from && item.to === newEdge.to) ||
                    (item.from === newEdge.to && item.to === newEdge.from);
            }
        });

        if (existingEdges.length > 0) {
            const idsToRemove = existingEdges.map(edge => edge.id);
            edges.remove(idsToRemove);
        }

        edges.add(newEdge);
        //network.fit();
    }

    element.updateNodeColor = (nodeId, colorCode) => {
        try {
            nodes.update({ id: nodeId, color: { background: colorCode } });
        } catch (e) { console.error("Errore update nodo", e); }
    };

    element.updateEdgeDirection = (fromId, toId) => {
        const items = edges.get({
            filter: function (item) {
                return (item.from === fromId && item.to === toId) ||
                    (item.from === toId && item.to === fromId);
            }
        });

        if (items.length > 0) {
            edges.remove(items[0].id);
            edges.add({ from: fromId, to: toId });
        }
    };

    element.fitGraph = () => {
        network.fit({
            animation: {
                duration: 1000,
                easingFunction: 'easeInOutQuad'
            }
        });
    };
};