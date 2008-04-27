package de.dal33t.powerfolder.ui.friends;

import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.Controller;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.factories.ButtonBarFactory;

public class FindUsersDialog extends BaseDialog {

    private FindUsersPanel panel;
    private JButton addToFriendsButton;
    private JButton okButton;

    public FindUsersDialog(Controller controller, boolean modal) {
        super(controller, modal);
        initComponents();
    }

    private void initComponents() {
        panel = new FindUsersPanel(getController());
        addToFriendsButton = createAddToFriendsButton();
        okButton = createOKButton();

    }

    private JButton createAddToFriendsButton() {        
        return new JButton(panel.getAddFriendAction());
    }

    public String getTitle() {
        return Translation.getTranslation("find_users_dialog.title");
    }

    protected Icon getIcon() {
        return Icons.SEARCH_NODES;
    }

    protected Component getContent() {
        return panel.getUIComponent();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(addToFriendsButton, okButton);
    }

    /**
     * Creates the okay button for the whole pref dialog
     */
    private JButton createOKButton() {
        return createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }
}
