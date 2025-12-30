#!/bin/bash

CP="build/install/node/lib/*"
MAIN="it.alesvale.node.NodeApplication"

NUM_NODES=${1:-3}

PIDS=()
TAIL_PID=""

cleanup() {
    echo ""
    echo "--- Arresto in corso ---"
    
    # Uccidi il processo di tail dei log se esiste
    if [ -n "$TAIL_PID" ]; then
        kill $TAIL_PID 2>/dev/null
    fi

    if [ ${#PIDS[@]} -gt 0 ]; then
        kill "${PIDS[@]}" 2>/dev/null
        wait "${PIDS[@]}" 2>/dev/null
    fi
    
    echo "Tutti i nodi terminati."
}

launch_node(){
  local NODE_ID=$1
  echo "Launching Node $NODE_ID..." >&2
  java -cp "$CP" $MAIN >> nodes.log 2>&1 &
  LAST_PID=$!
}

trap cleanup SIGINT

echo "Installing dependencies..."
cd /mnt/c/Users/alesv/IdeaProjects/raymond-distributed-ai/node || exit 1
./gradlew installDist

export BROKER_URL=nats://localhost:4222

rm nodes.log 2>/dev/null
touch nodes.log

echo "Avvio di $NUM_NODES nodi..."

# Ciclo per lanciare N nodi
for (( i=1; i<=NUM_NODES; i++ ))
do
   launch_node $i
   PIDS+=($LAST_PID)
done

echo "Tutti i nodi avviati. Pids: ${PIDS[*]}"

tail -f nodes.log &
TAIL_PID=$!

# Attende che tutti i processi nell'array terminino
wait "${PIDS[@]}"