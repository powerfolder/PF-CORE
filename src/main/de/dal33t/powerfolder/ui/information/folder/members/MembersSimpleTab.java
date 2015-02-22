/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: MembersTab.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.members;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;

/**
 * UI component for the members information tab
 */
public class MembersSimpleTab extends PFUIComponent implements MembersTab {

    private JPanel uiComponent;
    private MembersSimpleTableModel model;
    private JScrollPane scrollPane;
    private BaseAction inviteAction;
    private MembersSimpleTable membersTable;

    /**
     * Constructor
     *
     * @param controller
     */
    public MembersSimpleTab(Controller controller) {
        super(controller);
        model = new MembersSimpleTableModel(getController());
        model.sortBy(MembersSimpleTableModel.COL_USERNAME);
    }

    /*
     * (non-Javadoc)
     * @see
     * de.dal33t.powerfolder.ui.information.folder.members.MembersTab#setFolderInfo
     * (de.dal33t.powerfolder.light.FolderInfo)
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        model.setFolderInfo(folderInfo);
        inviteAction.allowWith(FolderPermission.admin(folderInfo));
    }

    /*
     * (non-Javadoc)
     * @see
     * de.dal33t.powerfolder.ui.information.folder.members.MembersTab#getUIComponent
     * ()
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    public void initialize() {
        inviteAction = new MyInviteAction();

        membersTable = new MembersSimpleTable(model);
        membersTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        membersTable.getSelectionModel().addListSelectionListener(
            new MySelectionListener());
        scrollPane = new JScrollPane(membersTable);

        // Whitestrip
        UIUtil.whiteStripTable(membersTable);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);

        enableOnSelection();

        model.getRefreshingModel().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean refreshing = (Boolean) evt.getNewValue();
                    boolean permissionsRetrieved = model
                        .isPermissionsRetrieved();
                    // Cancel edit of current cell
                    membersTable.cancelCellEditing();
                    boolean enabled = !refreshing && permissionsRetrieved;
                    FolderInfo folderInfo = model.getFolderInfo();
                    if (folderInfo != null) {
                        boolean admin = getController().getOSClient()
                            .getAccount().hasAdminPermission(folderInfo);
                        enabled = enabled && admin;
                    }
                }
            });
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(scrollPane, cc.xy(2, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton inviteButton = new JButton(inviteAction);

        FormLayout layout = new FormLayout("0:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        enableOnSelection();

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        if (ConfigurationEntry.SERVER_INVITE_ENABLED
            .getValueBoolean(getController()))
        {
            bar.addGridded(inviteButton);
        }
        JPanel buttonBarPanel = bar.getPanel();

        layout = new FormLayout("pref, 0:grow, pref", "pref");
        builder = new PanelBuilder(layout);
        cc = new CellConstraints();
        builder.add(buttonBarPanel, cc.xy(1, 1));
        builder.add(Help.createWikiLinkButton(getController(),
            WikiLinks.SECURITY_PERMISSION), cc.xy(3, 1));

        return builder.getPanel();
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
    }

    private static JComboBox createdEditComboBox(
        SelectionInList<FolderPermission> folderPermissions)
    {
        return BasicComponentFactory.createComboBox(folderPermissions,
            new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus)
                {
                    Component comp = super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
                    if (value instanceof FolderPermission) {
                        setText(((FolderPermission) value).getName());
                    } else {
                        setText(Translation
                            .get("permissions.folder.no_access"));
                    }
                    return comp;
                }
            });
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    /**
     * Class to detect table selection changes.
     */
    private class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableOnSelection();
        }
    }

    // Action to invite friend.
    private class MyInviteAction extends BaseAction {

        private MyInviteAction() {
            super("action_invite_friend", MembersSimpleTab.this.getController());
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openSendInvitationWizard(getController(),
                model.getFolderInfo());
            model.refreshModel();
        }
    }
}