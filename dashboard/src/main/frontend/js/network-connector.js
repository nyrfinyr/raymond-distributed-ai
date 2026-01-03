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
                strokeWidth: 2,
                strokeColor: '#ffffff'
            },
            borderWidth: 2,
            shadow: true,
            color: {
                background: '#97C2FC',
                border: '#2B7CE9',
                highlight: {
                    background: '#97C2FC',
                    border: '#2B7CE9'
                }
            }
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
            solver: 'barnesHut',
            barnesHut: {
                gravitationalConstant: -4000,
                centralGravity: 0.3,
                springLength: 250,
                springConstant: 0.04,
                damping: 0.09,
                avoidOverlap: 1
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

    const mapNode = (rawNode) => {
        return {
            ...rawNode,
            id: rawNode.nodeId.nodeId,
            label: rawNode.label
        };
    };

    const mapEdge = (rawEdge) => {
        return {
            ...rawEdge,
            from: rawEdge.from.nodeId,
            to: rawEdge.to.nodeId
        };
    };

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
            nodes.update({
                id: nodeId,
                color: {
                    background: colorCode,
                    highlight: {
                        background: colorCode,
                        border: '#2B7CE9'
                    },
                    hover: {
                        background: colorCode
                    }
                }
            });
        } catch (e) { console.error("Errore update nodo", e); }
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