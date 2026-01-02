package it.alesvale.node.data;

import it.alesvale.node.service.AgentService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StateMachine<T extends Enum<T>> {

    @Getter
    private T currentState;

    private final Map<T, Runnable> mapStateActions = new HashMap<>();

    public void onStateAction(T t, AgentService service) {
        mapStateActions.put(t, service::start);
    }

    public void setState(T targetState) {

        if (currentState == targetState) {
            return;
        }

        mapStateActions.getOrDefault(targetState, () -> {}).run();
        this.currentState = targetState;
    }
}


