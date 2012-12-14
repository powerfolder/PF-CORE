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
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class SendInvitationsAdvancedPanel extends BaseDialog {

    private JButton okButton;
    private JButton cancelButton;
    private final ValueModel permissionsValueModel;
    private final FolderInfo foInfo;
    private JComboBox permissionsCombo;

    public SendInvitationsAdvancedPanel(Controller controller,
        FolderInfo foInfo,
        ValueModel permissionsValueModel)
    {
        super(Senior.NONE, controller, true);
        Reject.ifNull(foInfo, "Folder info is null");
        this.permissionsValueModel = permissionsValueModel;
        this.foInfo = foInfo;
        initComponents();
    }

    private void initComponents() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        SelectionInList<FolderPermission> permissionsModel = new SelectionInList<FolderPermission>();
        permissionsModel.getList().add(FolderPermission.read(foInfo));
        permissionsModel.getList().add(FolderPermission.readWrite(foInfo));
        permissionsModel.getList().add(FolderPermission.admin(foInfo));
        FolderPermission fp = (FolderPermission) permissionsValueModel
            .getValue();
        permissionsModel.setSelectionHolder(permissionsValueModel);
        permissionsModel.setValue(fp);

        permissionsCombo = BasicComponentFactory.createComboBox(
            permissionsModel, new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus)
                {
                    Component comp = super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
                    if (value instanceof FolderPermission) {
                        FolderPermission fp = (FolderPermission) value;
                        setText(fp.getName());
                    }
                    return comp;
                }
            });
    }

    private void ok() {
        close();
    }

    private void cancel() {
        close();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    protected JComponent getContent() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(Translation.getTranslation("send_invitations_advanced.permissions_label"),
                cc.xy(1, 1));

        builder.add(new JLabel(Translation.getTranslation("send_invitations_advanced.permissions_hint")),
                cc.xy(1, 3));

        builder.add(permissionsCombo, cc.xy(1, 5));

        return builder.getPanel();
    }

    protected Icon getIcon() {
        return null;
    }

    public String getTitle() {
        return Translation.getTranslation("wizard.send_invitations_advanced.title");
    }

}