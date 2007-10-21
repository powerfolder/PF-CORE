package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.clientserver.EchoRequest;
import de.dal33t.powerfolder.message.clientserver.EchoResponse;

/**
 * Answers the EchoRequest.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class EchoHandler extends PFComponent {

    public EchoHandler(Controller controller) {
        super(controller);
        controller.getNodeManager().addMessageListenerToAllNodes(
            new MyMessageListner());
    }

    private class MyMessageListner implements MessageListener {
        public void handleMessage(Member source, Message message) {
            if (!(message instanceof EchoRequest)) {
                return;
            }
            EchoRequest request = (EchoRequest) message;
            EchoResponse response = new EchoResponse();
            response.associate(request);
            response.payload = request.payload;
            source.sendMessagesAsynchron(response);
        }
    }
}
