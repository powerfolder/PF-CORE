package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;

@SuppressWarnings("serial")
public class CreateShortcutAction extends BaseAction {


    public CreateShortcutAction(Controller controller) {
        super("createshortcut", controller);
    }

    public void actionPerformed(ActionEvent evt) {
        Object selectedItem = getUIController().getControlQuarter()
        .getSelectedItem();
        if (selectedItem instanceof Folder) {
            Folder folder = (Folder) selectedItem;
            folder.setDesktopShortcut(true);
        }    
    }
}
