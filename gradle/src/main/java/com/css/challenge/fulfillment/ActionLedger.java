package com.css.challenge.fulfillment;

import com.css.challenge.client.Action;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionLedger {
    private final List<Action> actions = Collections.synchronizedList(new ArrayList<>());

    public void record(String id, String actionType, String target) {
        actions.add(new Action(Instant.now(), id, actionType, target));
    }

    public List<Action> getActions() {
        return new ArrayList<>(actions);
    }

}
