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
 * $Id: NoticesCard.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.notices;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationCardType;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.util.Translation;

public class NoticesInformationCard extends InformationCard {

    private JPanel uiComponent;
    private NoticesModel model;
    private NoticesTableModel noticesTableModel;
    private NoticesTable noticesTable;
    private Action activateAction;
    private JPopupMenu actionsMenu;

    public NoticesInformationCard(Controller controller) {
        super(controller);
        model = controller.getUIController().getApplicationModel()
            .getNoticesModel();
    }

    public InformationCardType getInformationCardType() {
        return InformationCardType.NOTICES;
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        actionsMenu = new JPopupMenu();
        actionsMenu.add(activateAction);
        actionsMenu.add(new CleanupNoticesAction(getController()));
    }

    public Image getCardImage() {
        return Icons.getImageById(Icons.INFORMATION);
    }

    public String getCardTitle() {
        return Translation.getTranslation("dialog.notices.title");
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        PropertyChangeListener noticesListener = new MyPropertyChangeListener();
        getController().getUIController().getApplicationModel()
            .getNoticesModel().getAllNoticesCountVM()
            .addValueChangeListener(noticesListener);
        noticesTableModel = new NoticesTableModel(getController());
        noticesTable = new NoticesTable(noticesTableModel);
        noticesTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    enableActivate();
                }
            });

        noticesTable.addMouseListener(new TableMouseListener());
        activateAction = new ActivateNoticeAction(getController());

        enableActivate();
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu");
        // tools sep table dets sep stats
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JScrollPane pane = new JScrollPane(noticesTable);
        builder.add(buildToolbar().getPanel(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(pane, cc.xy(2, 6));
        uiComponent = builder.getPanel();
        buildPopupMenus();
    }

    /**
     * Build the toolbar component.
     */
    private ButtonBarBuilder buildToolbar() {
        JButton activateButton = new JButton(activateAction);
        JButton clearAllButton = new JButton(new CleanupNoticesAction(
            getController()));

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(activateButton);
        bar.addRelatedGap();
        bar.addGridded(clearAllButton);
        return bar;
    }

    private void updateTableModel() {
        noticesTableModel.reset();
    }

    private void enableActivate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                activateAction
                    .setEnabled(noticesTable.getSelectedRowCount() > 0);
            }
        });
    }

    /**
     * When a user double-clicks a row
     */
    private void activateNotice() {
        int row = noticesTable.getSelectedRow();
        if (row >= 0) {
            Object at = noticesTableModel.getValueAt(row, 0);
            if (at != null && at instanceof Notice) {
                Notice notice = (Notice) at;
                model.activateNotice(notice);
            }
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class CleanupNoticesAction extends BaseAction {

        CleanupNoticesAction(Controller controller) {
            super("action_cleanup_notices", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int row = noticesTable.getSelectedRow();
            if (row < 0) {
                getController().getUIController().getApplicationModel()
                    .getNoticesModel().clearAll();
            } else {
                Object at = noticesTableModel.getValueAt(row, 0);
                if (at != null && at instanceof Notice) {
                    Notice notice = (Notice) at;
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().clearNotice(notice);
                }
            }
        }
    }

    private class ActivateNoticeAction extends BaseAction {

        ActivateNoticeAction(Controller controller) {
            super("action_activate_notice", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int row = noticesTable.getSelectedRow();
            if (row >= 0) {
                Object at = noticesTableModel.getValueAt(row, 0);
                if (at != null && at instanceof Notice) {
                    Notice notice = (Notice) at;
                    model.activateNotice(notice);
                }
            }
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateTableModel();
        }
    }

    private class TableMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    activateNotice();
                }
            }
        }

        private void showContextMenu(MouseEvent evt) {
            actionsMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }
}
