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

    static NodeStateMachine build() {
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(NodeStateMachine.class);
        // Handshake
        builder.externalTransition().from(NodeState.LISTEN).to(NodeState.OPEN_IDENTITY_REPLY_WAIT).on(NodeEvent.IDENTITY).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_IDENTITY_REPLY_WAIT).to(NodeState.OPEN_LOGIN_REQUEST_WAIT).on(NodeEvent.IDENTITY_REPLY).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_LOGIN_REQUEST_WAIT).to(NodeState.OPEN_ACCOUNT_INFO_REQUEST_WAIT).on(NodeEvent.LOGIN_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_ACCOUNT_INFO_REQUEST_WAIT).to(NodeState.OPEN_FOLDER_LIST_WAIT).on(NodeEvent.ACCOUNT_INFO_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_FOLDER_LIST_WAIT).to(NodeState.OPEN_HANDSHAKE_COMPLETED_WAIT).on(NodeEvent.FOLDER_LIST).callMethod("handle");
        builder.externalTransition().from(NodeState.OPEN_HANDSHAKE_COMPLETED_WAIT).to(NodeState.ESTABLISHED).on(NodeEvent.HANDSHAKE_COMPLETED).callMethod("handle");
        // Established
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.ACCOUNT_CHANGE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.ACCOUNT_INFO_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.ACCOUNT_SEARCH_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.ACTIVITY_LIST_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.AVATAR_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.CERTIFICATE_SIGNING_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.DOWNLOAD_ABORT).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.DOWNLOAD_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FILE_LIST_REPLY).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FILE_LIST_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FILE_PART_REPLY).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FILE_PART_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FILE_SEARCH_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FOLDER_CREATE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FOLDER_FILES_CHANGED).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FOLDER_REMOVE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FOLDER_RENAME_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.FOLDER_SERVER_NODES_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.GROUP_SEARCH_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.INVITATION_ACCEPT_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.INVITATION_CREATE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PING).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PONG).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PERMISSION_CHANGE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PERMISSION_INFO_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PERMISSION_LIST_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.PERMISSION_REMOVE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.SHARE_LINK_CHANGE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.SHARE_LINK_CREATE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.SHARE_LINK_INFO_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.SHARE_LINK_LIST_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.SHARE_LINK_REMOVE_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.THUMBNAIL_REQUEST).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.UPLOAD_ABORT).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.UPLOAD_START).callMethod("handle");
        builder.externalTransition().from(NodeState.ESTABLISHED).to(NodeState.ESTABLISHED).on(NodeEvent.UPLOAD_STOP).callMethod("handle");

        // Always allow
        //builder.externalTransition().from("*").to("*").on(NodeEvent.PERMISSION_LIST_REQUEST).callMethod("handle");
        return (NodeStateMachine) builder.newStateMachine(NodeState.LISTEN);
    }

}
