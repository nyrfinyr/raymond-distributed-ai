import { Network } from "vis-network/peer/esm/vis-network";
import { DataSet } from "vis-data/peer/esm/vis-data";

window.initNetworkGraph = (element) => {
    const nodes = new DataSet([]);
    const edges = new DataSet([]);

    const options = {
        nodes: {
            shape: 'dot',
            size: 30,
            font: {
                size: 16,
                color: '#000000',
                strokeWidth: 2, // Bordo bianco al testo per leggerlo sopra le linee
                strokeColor: '#ffffff'
            },
            borderWidth: 2,
            shadow: true
        },
        edges: {
            width: 2,
            color: { color: '#848484', highlight: '#848484'},
            arrows: 'to',
            smooth: {
                type: 'dynamic',
                roundness: 0.5
            }
        },
        physics: {
            enabled: true,
            solver: 'barnesHut', // Solver più stabile per evitare overlapping
            barnesHut: {
                gravitationalConstant: -4000, // Molto negativo = forte repulsione tra nodi
                centralGravity: 0.3,          // Tira i nodi verso il centro per non farli disperdere troppo
                springLength: 250,            // Lunghezza ideale dei collegamenti (aumenta lo spazio)
                springConstant: 0.04,         // Rigidità delle molle
                damping: 0.09,                // Smorzamento per stabilizzare
                avoidOverlap: 1               // Forza specifica anti-sovrapposizione (0 a 1)
            },
            stabilization: {
                enabled: true,
                iterations: 1000,
                updateInterval: 50,
                onlyDynamicEdges: false,
                fit: true
            },
            adaptiveTimestep: true
        },
        layout: {
            improvedLayout: true,
            randomSeed: 42
        },
        interaction: {
            dragNodes: true,
            zoomView: true,
            hover: true
        }
    };

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

    // Event listener per il click sui nodi
    network.on('click', function(params) {
        if (params.nodes.length > 0) {
            const nodeId = params.nodes[0];
            console.log("onClick: " + nodeId);
            element.$server.onNodeClicked(nodeId);
        }
    });

    element.setGraphData = (nodesJson, edgesJson) => {
        const rawNodes = JSON.parse(nodesJson);
        const rawEdges = JSON.parse(edgesJson);
        nodes.clear();
        edges.clear();
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

        const exactMatch = edges.get({
            filter: function (item) {
                return item.from === newEdge.from && item.to === newEdge.to;
            }
        });

        if (exactMatch.length > 0) {
            return;
        }

        const reverseMatch = edges.get({
            filter: function (item) {
                return item.from === newEdge.to && item.to === newEdge.from;
            }
        });

        if (reverseMatch.length > 0) {
            edges.remove(reverseMatch.map(e => e.id));
        }

        edges.add(newEdge);
    }

    element.updateNodeColor = (nodeId, colorCode) => {
        try {
            nodes.update({ id: nodeId, color: { background: colorCode } });
        } catch (e) { console.error("Errore update nodo", e); }
    };

    element.updateEdgeDirection = (fromId, toId) => {

        const existingCorrect = edges.get({
            filter: function (item) {
                return item.from === fromId && item.to === toId;
            }
        });

        if (existingCorrect.length > 0) {
            return;
        }

        const existingReverse = edges.get({
            filter: function (item) {
                return item.from === toId && item.to === fromId;
            }
        });

        if (existingReverse.length > 0) {
            edges.remove(existingReverse[0].id);
            edges.add({ from: fromId, to: toId });
        } else {
            edges.add({ from: fromId, to: toId });
        }
    };

    element.fitGraph = () => {
        if (network) {
            network.fit({
                animation: {
                    duration: 1000,
                    easingFunction: 'easeInOutQuad'
                }
            });
        }
    };
};