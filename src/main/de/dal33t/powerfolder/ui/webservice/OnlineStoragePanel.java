/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.ui.webservice;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.FolderListPanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class OnlineStoragePanel extends PFUIPanel {
    private ServerClientModel model;
    private JComponent panel;

    private QuickInfoPanel quickInfo;
    private JComponent toolbar;
    private JLabel accountNameLabel;
    private JLabel changeAccountLabel;

    private Component foldersListPanel;
    private SelectionInList foldersListModel;

    public OnlineStoragePanel(Controller controller, ServerClientModel model) {
        super(controller);
        Reject.ifNull(model, "model is null");
        this.model = model;
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * TODO #495
     * 
     * @return the title
     */
    public String getTitle() {
        return Translation.getTranslation("general.webservice");
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow", "fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(foldersListPanel, cc.xy(1, 1));
        return builder.getPanel();
    }

    private void initComponents() {
        quickInfo = new OnlineStorageQuickInfoPanel(getController());

        ValueModel usernameModel = model.getAccountModel().getModel(
            Account.PROPERTYNAME_USERNAME);
        accountNameLabel = BasicComponentFactory.createLabel(usernameModel);

        changeAccountLabel = new ActionLabel(new BaseAction(
            "online_storage_change_account", getController())
        {
            public void actionPerformed(ActionEvent e) {
                PFWizard.openLoginWebServiceWizard(getController(), model
                    .getClient(), false);
            }
        });
        updateChangeAccountLabel();
        getController().getOSClient().addListener(new MyServerClientListner());

        foldersListModel = new SelectionInList(model.getMirroredFoldersModel());
        foldersListPanel = new FolderListPanel(foldersListModel)
            .getUIComponent();

        // Create toolbar
        toolbar = createToolBar();
    }

    private void updateChangeAccountLabel() {
        if (model.getClient().isConnected()) {
            changeAccountLabel.setText(Translation
                .getTranslation("online_storage_change_account.name"));
        } else {
            changeAccountLabel.setText("");
        }
    }

    /**
     * @return the toolbar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(new OpenWebServiceInBrowserAction(
            getController())));
        bar.addRelatedGap();
        JButton mirrorButton = new JButton(model.getMirrorFolderAction());
        bar.addGridded(mirrorButton);
        mirrorButton.setEnabled(!getController().isLanOnly());
        // bar.addRelatedGap();
        // bar.addGridded(new JButton(new SyncFolderRightsAction(getController()
        // .getOSClient())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(new AboutWebServiceAction(getController())));

        // bar.addRelatedGap();
        // bar.addGridded(new JButton(clearCompletedAction));

        bar.addUnrelatedGap();
        bar.addFixed(accountNameLabel);
        bar.addRelatedGap();
        bar.addFixed(changeAccountLabel);

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    private class MyServerClientListner implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            updateChangeAccountLabel();
        }

        public void login(ServerClientEvent event) {
            updateChangeAccountLabel();
        }

        public void serverConnected(ServerClientEvent event) {
            updateChangeAccountLabel();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateChangeAccountLabel();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
