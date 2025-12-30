CP="build/install/node/lib/*"
MAIN="it.alesvale.node.NodeApplication"

cleanup() {
    echo ""
    echo "--- Arresto in corso ---"
    kill $PID1 $PID2 $PID3
    wait $PID1 $PID2 $PID3 2>/dev/null
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

rm nodes.log

launch_node 1
PID1=$LAST_PID

launch_node 2
PID2=$LAST_PID

launch_node 3
PID3=$LAST_PID

echo "pids: $PID1, $PID2, $PID3"

touch nodes.log
tail -f nodes.log &

wait $PID1 $PID2 $PID3