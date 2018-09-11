package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.Member;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

@StateMachineParameters(stateType = NodeState.class, eventType = NodeEvent.class, contextType = D2DObject.class)
public class NodeStateMachine extends AbstractUntypedStateMachine {

    private Member node;

    public void setNode(Member node) {
        this.node = node;
    }

    protected void handle(NodeState from, NodeState to, NodeEvent event, D2DObject d2DObject) {
        d2DObject.handle(this.node);
    }

    static NodeStateMachine build(Member node) {
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(NodeStateMachine.class);
        // Handshake
        builder.externalTransition().from(NodeState.LISTEN).to(NodeState.OPEN_IDENTITY_REPLY_WAIT).on(NodeEvent.IDENTITY).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_IDENTITY_REPLY_WAIT).to(NodeState.OPEN_LOGIN_REQUEST_WAIT).on(NodeEvent.IDENTITY_REPLY).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_LOGIN_REQUEST_WAIT).to(NodeState.OPEN_ACCOUNT_INFO_REQUEST_WAIT).on(NodeEvent.LOGIN_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_ACCOUNT_INFO_REQUEST_WAIT).to(NodeState.OPEN_FOLDER_LIST_WAIT).on(NodeEvent.ACCOUNT_INFO_REQUEST).callMethod("handle");
        // Always allow
        //builder.externalTransition().from("*").to("*").on(NodeEvent.PERMISSION_LIST_REQUEST).callMethod("handle");
        NodeStateMachine nodeStateMachine = (NodeStateMachine) builder.newStateMachine(NodeState.LISTEN);
        nodeStateMachine.node = node;
        return nodeStateMachine;
    }

}
