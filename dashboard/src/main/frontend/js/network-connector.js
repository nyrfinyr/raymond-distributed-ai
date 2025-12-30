import { Network } from "vis-network/peer/esm/vis-network";
import { DataSet } from "vis-data/peer/esm/vis-data";

window.initNetworkGraph = (element) => {
    const nodes = new DataSet([]);
    const edges = new DataSet([]);

    // 2. Configurazione Vis.js
    const options = {
        nodes: {
            shape: 'dot',
            size: 30,
            font: { 
                size: 16, 
                color: '#000000',
                face: 'arial',
                strokeWidth: 3,
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
                type: 'cubicBezier', 
                forceDirection: 'vertical', 
                roundness: 0.4 
            } 
        },
        physics: {
            enabled: false 
        },
        layout: {
            hierarchical: {
                enabled: true,
                direction: 'DU',        
                sortMethod: 'directed', 
                nodeSpacing: 250,
                levelSeparation: 200,
                
                blockShifting: true,
                edgeMinimization: true,
                parentCentralization: true,
                shakeTowards: 'roots'   
            }
        },
        interaction: { dragNodes: true, zoomView: true }
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
        network.fit({
            animation: {
                duration: 1000,
                easingFunction: 'easeInOutQuad'
            }
        });
    };
};