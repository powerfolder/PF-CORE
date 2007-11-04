/* $Id: ChangeFriendStatusAction.java,v 1.14 2006/02/20 11:37:32 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Actions which switches the friend status of a member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class ChangeFriendStatusAction extends SelectionBaseAction {

    public ChangeFriendStatusAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("addfriend", controller, selectionModel);
        Object selection = selectionModel.getSelection();
        if (selection instanceof Member) {
            adaptFor((Member) selection);
        } else if (selection instanceof FileInfo) {
            adaptFor((FileInfo) selection);
        } else {
            setEnabled(false);
        }
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof Member) {
            adaptFor((Member) selection);
        } else if (selection instanceof FileInfo) {
            adaptFor((FileInfo) selection);
        } else {
            setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object selection = getSelectionModel().getSelection();

        log().verbose("Performing on item: " + selection);
        Member member = null;

        if (selection instanceof Member) {
            member = (Member) selection;
        } else if (selection instanceof FileInfo) {
            FileInfo file = (FileInfo) selection;
            member = file.getModifiedBy().getNode(getController(), true);
        }

        // Switch friend status
        if (member != null) {
            member.setFriend(!member.isFriend());
            adaptFor(member);
        } else {
            log().warn("Unable to change friend status, member not found");
        }
    }

    private void configureButton(boolean isFriend) {
        if (isFriend) {
            configureFromActionId("removefriend");
        } else {
            configureFromActionId("addfriend");
        }
    }

    /**
     * Adapts the action text and icon for the member
     * 
     * @param member
     */
    private void adaptFor(Member member) {
        setEnabled(!member.isMySelf());
        configureButton(member.isFriend());
    }

    /**
     * Adapts action text and icon for a members file
     * 
     * @param file
     */
    private void adaptFor(FileInfo file) {
        MemberInfo member = file.getModifiedBy();
        // Exclude myself
        setEnabled(!member.matches(getController().getMySelf()));
        configureButton(member.isFriend(getController()));
    }
}