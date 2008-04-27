package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.friends.FindUsersDialog;

import java.awt.event.ActionEvent;

public class FindFriendAction extends BaseAction {
    public FindFriendAction(Controller controller) {
        super("findfriends", controller);
    }

    public void actionPerformed(ActionEvent e) {
        FindUsersDialog dialog = new FindUsersDialog(getController(), true);
        dialog.open();
    }
}

