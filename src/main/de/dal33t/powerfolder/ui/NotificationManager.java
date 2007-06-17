package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.MemberChatPanel;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelListener;
import de.dal33t.powerfolder.util.ui.SlidingInfoMessageBox;

/**
 * Listens to model events and brings up the animated sliding notification view.
 *  
 * @author <A HREF="mailto:magapov@gmail.com">Maxim Agapov</A>
 * @version $Revision: 1.0 $
 */
public class NotificationManager extends PFUIComponent {
    
    /**
     * create a NotificationManager. 
     * 
     * @param controller
     *            the controller
     * @param chatModel
     *            the chat model to act upon
     */
    public NotificationManager(Controller controller) {
        super(controller);
        ChatModel chatModel = getUIController().getChatModel();
        if (chatModel == null) {
            throw new IllegalStateException("NotificationManager: chatModel is null");
        }
        chatModel.addChatModelListener(new MyChatModelListener());
    }

    // Internal classes ********************************************************

    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            if (event.isStatus()) {
                // Ignore status updates
                return;
            }

            if (event.getSource() instanceof Member) {
                Member chatMessageMember = (Member) event.getSource();
                if (!(getUIController().getInformationQuarter()
                    .getDisplayTarget() instanceof MemberChatPanel))
                {
                    new SlidingInfoMessageBox().show("New msg received from\n" + chatMessageMember);
                }
            }
        }
    }
}
