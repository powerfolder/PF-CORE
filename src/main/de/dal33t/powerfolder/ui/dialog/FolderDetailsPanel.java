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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;

/**
 * A Information panel for a folder. Displays most important things
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderDetailsPanel extends PFUIComponent {
    private FolderInfo foInfo;
    private Folder folder;
    private JPanel panel;

    private JLabel nameField;
    private JTextField sizeField;
    private JLabel totalSyncField;
    private JLabel syncProfileField;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JTextField localCopyAtField;

    private FolderStatisticListener statisticListener;

    /**
     * Initalizes panel with the given ValueModel, holding the folder. Listens
     * on changes of the model
     * 
     * @param controller
     * @param folderModel
     *            the model containing the folder
     */
    public FolderDetailsPanel(Controller controller, ValueModel folderModel) {
        super(controller);

        if (folderModel == null) {
            throw new NullPointerException("Folder model is null");
        }
        folderModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() instanceof FolderInfo) {
                    setFolder((FolderInfo) evt.getNewValue());
                } else if (evt.getNewValue() instanceof Folder) {
                    setFolder(((Folder) evt.getNewValue()).getInfo());
                }
            }
        });
        
        if (folderModel.getValue() instanceof FolderInfo) {
            setFolder((FolderInfo) folderModel.getValue());
        } else if (folderModel.getValue() instanceof Folder) {
            setFolder(((Folder) folderModel.getValue()).getInfo());
        }

        // Init listener
        this.statisticListener = new FolderStatisticListener();
    }

    // Setter/Getter **********************************************************

    /**
     * @return the folder, which is currently displayed
     */
    public FolderInfo getFolder() {
        return foInfo;
    }

    /**
     * Sets the infors about a folder.
     * 
     * @param aFoInfo
     */
    private void setFolder(FolderInfo aFoInfo) {
        if (aFoInfo == null) {
            throw new NullPointerException("Folder may not be null");
        }

        // Remove listener from old folder
        if (foInfo != null) {
            Folder oldFolder = foInfo.getFolder(getController());
            if (oldFolder != null) {
                oldFolder.removeFolderListener(statisticListener);
            }
        }

        // Get new folders
        this.foInfo = aFoInfo;
        this.folder = aFoInfo.getFolder(getController());

        // Add update listener
        if (folder != null) {
            folder.addFolderListener(statisticListener);
        }

        if (panel == null) {
            // Panel not initalizes yet
            return;
        }

        nameField.setText(foInfo.name);
        nameField.setIcon(Icons.FOLDER);

        long bytesTotal = foInfo.bytesTotal;
        int filesCount = foInfo.filesCount;
        if (folder != null) {
            bytesTotal = folder.getStatistic().getTotalSize();
            filesCount = folder.getStatistic().getTotalFilesCount();
        }
        sizeField.setText(filesCount + " "
            + Translation.getTranslation("general.files") + " ("
            + Format.formatBytes(bytesTotal) + ")");
        sizeField.setCaretPosition(0);

        if (folder != null) {
            syncProfileField.setText(folder.getSyncProfile().getProfileName());

            double sync = folder.getStatistic().getHarmonizedSyncPercentage();
            totalSyncField.setText(SyncProfileUtil.renderSyncPercentage(sync));
            totalSyncField.setIcon(SyncProfileUtil.getSyncIcon(sync));

            // syncProfileModel.setValue(folder.getSyncProfile());

            localCopyAtField.setText(folder.getLocalBase().getAbsolutePath());
        } else {
            String naText = "- "
                + Translation.getTranslation("general.notavailable") + " -";

            totalSyncField.setText(naText);
            totalSyncField.setIcon(Icons.FOLDER_SYNC_UNKNOWN);
            syncProfileField.setText(naText);
            //syncProfileModel.setValue(null);
            localCopyAtField.setText(naText);
        }

        localCopyAtField.setCaretPosition(0);
    }

    // Upating code ***********************************************************

    /**
     * Listens for changes on the statistic of a folder and trigger the update
     * of this panel
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.9 $
     */
    private class FolderStatisticListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            // Update folder
            logFiner("Statistic has been recalc on " + folderEvent);
            setFolder(foInfo);
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    // UI Methods *************************************************************

    /**
     * Returns the panel. initalized lazily
     * 
     * @return the panel
     */
    public JPanel getPanel() {
        if (panel == null) {
            // initalize UI elements
            initComponents();

            FormLayout layout = new FormLayout("right:pref, 7dlu, pref:grow",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout, panel);
            CellConstraints cc = new CellConstraints();

            // Top
            builder.addLabel(Translation.getTranslation("general.foldername"),
                cc.xy(1, 1)).setForeground(Color.BLACK);
            builder.add(nameField, cc.xy(3, 1));

            builder.addLabel(Translation.getTranslation("general.size"),
                cc.xy(1, 3)).setForeground(Color.BLACK);
            builder.add(sizeField, cc.xy(3, 3));

            builder
                .addLabel(Translation.getTranslation("folderinfo.totalsync"),
                    cc.xy(1, 5)).setForeground(Color.BLACK);
            builder.add(totalSyncField, cc.xy(3, 5));

            builder.addLabel(
                Translation.getTranslation("folderinfo.syncprofile"),
                cc.xy(1, 7)).setForeground(Color.BLACK);
            builder.add(syncProfileSelectorPanel.getUIComponent(), cc.xy(3, 7));

            // Bottom
            builder.addLabel(Translation.getTranslation("general.local_copy_at"),
                cc.xy(1, 9)).setForeground(Color.BLACK);
            builder.add(localCopyAtField, cc.xy(3, 9));

            if (foInfo != null) {
                setFolder(foInfo);
            }
        }

        return panel;
    }

    /**
     * Initalizes all needed components
     */
    private void initComponents() {
        panel = new JPanel();

        nameField = SimpleComponentFactory.createLabel();
        nameField.setForeground(Color.BLACK);
        ensureDims(nameField);

        sizeField = SimpleComponentFactory.createTextField(false);
        ensureDims(sizeField);

        totalSyncField = SimpleComponentFactory.createLabel();
        totalSyncField.setForeground(Color.BLACK);
        ensureDims(totalSyncField);

        syncProfileField = SimpleComponentFactory.createLabel();
        syncProfileField.setForeground(Color.BLACK);
        ensureDims(syncProfileField);

        syncProfileSelectorPanel = new SyncProfileSelectorPanel(null);
        ensureDims((JComponent) syncProfileSelectorPanel.getUIComponent());

        localCopyAtField = SimpleComponentFactory.createTextField(false);
        ensureDims(localCopyAtField);
    }

    /**
     * Ensures the preferred height and widht of a component
     * 
     * @param label
     */
    private void ensureDims(JComponent comp) {
        Dimension dims = comp.getPreferredSize();
        dims.height = Math.max(dims.height, Icons.FOLDER.getIconHeight());
        dims.width = 10;
        comp.setPreferredSize(dims);

    }
}