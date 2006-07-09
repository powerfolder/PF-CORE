/* $Id: SetMasterNodeAction.java,v 1.3 2005/08/03 21:07:56 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.util.Translation;

/**
 * Action which offers the user the possibility to set a master node
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class SetMasterNodeAction extends BaseAction {

    public SetMasterNodeAction(Controller controller) {
        super("setmasternode", controller);
    }

    public void actionPerformed(ActionEvent e) {
        Member[] nodes = getController().getNodeManager().getNodes();
        List canidates = new ArrayList();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].isCompleteyConnected()) {
                canidates.add(new MemberWrapper(nodes[i]));
            }
        }

        Object none = Translation.getTranslation("masternode.nobody");
        canidates.add(0, none);

        Member masterNode = getController().getNodeManager().getMasterNode();

        Object result = JOptionPane.showInputDialog(getUIController()
            .getMainFrame().getUIComponent(), Translation
            .getTranslation("setmasternode.dialog.text"), Translation
            .getTranslation("setmasternode.dialog.title"),
            JOptionPane.QUESTION_MESSAGE, (Icon) getValue(Action.SMALL_ICON),
            canidates.toArray(), masterNode != null ? new MemberWrapper(
                masterNode) : none);

        if (result != null) {
            // Not canceled
            if (result == none) {
                // No master
                ConfigurationEntry.MASTER_NODE_ID.removeValue(getController());
            } else {
                MemberWrapper choosenMaster = (MemberWrapper) result;
                ConfigurationEntry.MASTER_NODE_ID.setValue(getController(),
                    choosenMaster.member.getId());

                // Request his nodelist, to get in sync
                choosenMaster.member.sendMessageAsynchron(
                    new RequestNodeList(), null);
            }

            getController().saveConfig();
        }

    }

    /**
     * TODO: Consolidate this into an own clasas
     * <p>
     * Helper class for option pane
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class MemberWrapper {
        private Member member;

        private MemberWrapper(Member member) {
            this.member = member;
        }

        public Member getMember() {
            return member;
        }

        public boolean equals(Object other) {
            if (other instanceof MemberWrapper) {
                return ((MemberWrapper) other).member.equals(member);
            } else if (other instanceof Member) {
                return ((Member) other).equals(member);
            }
            return false;
        }

        public int hashCode() {
            return member.hashCode();
        }

        public String toString() {
            return member.getNick() + " ("
                + (member.isOnLAN() ? "local" : "i-net") + ")";
        }
    }
}