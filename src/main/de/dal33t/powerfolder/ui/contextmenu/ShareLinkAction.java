package de.dal33t.powerfolder.ui.contextmenu;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.Controller;

class ShareLinkAction extends ContextMenuAction {

    private Controller controller;

    ShareLinkAction(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void onSelection(String[] paths) {
        // TODO Auto-generated method stub

    }

}
