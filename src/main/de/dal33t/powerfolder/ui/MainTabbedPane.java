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
 * $Id: MainTabbedPane.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.computers.ComputersTab;
import de.dal33t.powerfolder.ui.folders.FoldersTab;
import de.dal33t.powerfolder.ui.status.StatusTab;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Translation;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * This is the main tabbed pain component in the PowerFolder GUI. In expert
 * mode, it shows the status, folders and computers tab. In non-expert mode, it
 * just returns the FolderTab.
 */
public class MainTabbedPane extends PFUIComponent {

    public static final int STATUS_INDEX = 0;
    public static final int FOLDERS_INDEX = 1;
    public static final int COMPUTERS_INDEX = 2;

    private StatusTab statusTab;
    private FoldersTab foldersTab;
    private ComputersTab computersTab;

    private JTabbedPane tabbedPane;

    private final boolean showComputersTab;
    private final boolean expertMode;
    private final AtomicBoolean initialized;

    /**
     * Constructor. Creates the main tabbed pane.
     * 
     * @param controller
     */
    public MainTabbedPane(Controller controller) {
        super(controller);
        initialized = new AtomicBoolean();
        showComputersTab = !getController().isBackupOnly();
        expertMode = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController());
    }

    /**
     * @return the ui main tabbed pane.
     */
    public JComponent getUIComponent() {

        if (!initialized.getAndSet(true)) {
            // Initalize components
            initComponents();

            if (expertMode) {

                tabbedPane.add(
                    Translation.getTranslation("main_tabbed_pane.status.name"),
                    statusTab.getUIComponent());

                tabbedPane
                    .add(Translation
                        .getTranslation("main_tabbed_pane.folders.name"),
                        foldersTab.getUIComponent());

                if (showComputersTab) {
                    tabbedPane.add(Translation
                        .getTranslation("main_tabbed_pane.computers.name"),
                        computersTab.getUIComponent());
                } else {
                    // Do not display computers tab in backup only mode, BUT
                    // need to create it anyways to prevent UI events breaking.
                    computersTab.getUIComponent();
                }

                String key = Translation
                    .getTranslation("main_tabbed_pane.status.key");
                tabbedPane.setMnemonicAt(STATUS_INDEX,
                    (int) Character.toUpperCase(key.charAt(0)));
                tabbedPane.setToolTipTextAt(STATUS_INDEX, Translation
                    .getTranslation("main_tabbed_pane.status.description"));
                // tabbedPane.setIconAt(STATUS_INDEX,
                // Icons.getIconById(Icons.STATUS));

                key = Translation
                    .getTranslation("main_tabbed_pane.folders.key");
                tabbedPane.setMnemonicAt(FOLDERS_INDEX,
                    (int) Character.toUpperCase(key.charAt(0)));
                tabbedPane.setToolTipTextAt(FOLDERS_INDEX, Translation
                    .getTranslation("main_tabbed_pane.folders.description"));
                // tabbedPane.setIconAt(FOLDERS_INDEX,
                // Icons.getIconById(Icons.FOLDER));

                if (showComputersTab) {
                    key = Translation
                        .getTranslation("main_tabbed_pane.computers.key");
                    tabbedPane.setMnemonicAt(COMPUTERS_INDEX,
                        (int) Character.toUpperCase(key.charAt(0)));
                    tabbedPane.setToolTipTextAt(COMPUTERS_INDEX,
                        Translation.getTranslation(""
                            + "main_tabbed_pane.computers.description"));
                    // tabbedPane.setIconAt(COMPUTERS_INDEX,
                    // Icons.getIconById(Icons.COMPUTER));
                }

                tabbedPane.addChangeListener(new MyChangelistener());

                int minWidth = PreferencesEntry.MAIN_FRAME_WIDTH
                    .getDefaultValueInt();
                int minHeight = PreferencesEntry.MAIN_FRAME_HEIGHT
                    .getDefaultValueInt();
                tabbedPane.setMinimumSize(new Dimension(minWidth, minHeight));

                CursorUtils.setHandCursor(tabbedPane);
                CursorUtils.setDefaultCursor(statusTab.getUIComponent());
                CursorUtils.setDefaultCursor(foldersTab.getUIComponent());
                CursorUtils.setDefaultCursor(computersTab.getUIComponent());

                setActiveTab(FOLDERS_INDEX);
            } else {
                foldersTab.populate();
            }
        }

        if (expertMode) {
            return tabbedPane;
        } else {
            return foldersTab.getUIComponent();
        }
    }

    public int getSelectedTabIndex() {
        if (expertMode) {
            return tabbedPane.getSelectedIndex();
        } else {
            // Why is someone asking for the tab index,
            // when only the folder tab is showing?
            throw new IllegalStateException("Expert mode == " + expertMode);
        }
    }

    /**
     * Initialize the components of the pane.
     */
    private void initComponents() {
        foldersTab = new FoldersTab(getController());
        if (expertMode) {
            tabbedPane = new JTabbedPane();
            tabbedPane.setOpaque(false);
            statusTab = new StatusTab(getController());
            computersTab = new ComputersTab(getController());
        }
    }

    /**
     * Add a change listener to the main tabbed pane.
     * 
     * @param l
     */
    public void addTabbedPaneChangeListener(ChangeListener l) {
        if (expertMode) {
            tabbedPane.addChangeListener(l);
        }
    }

    /**
     * Remove a change listener from the main tabbed pane.
     * 
     * @param l
     */
    public void removeTabbedPaneChangeListener(ChangeListener l) {
        if (expertMode) {
            tabbedPane.removeChangeListener(l);
        }
    }

    /**
     * Set the status tab icon.
     * 
     * @param statusIcon
     */
    public void setStatusIcon(Icon statusIcon) {
        if (expertMode) {
            // tabbedPane.setIconAt(STATUS_INDEX, statusIcon);
        }
    }

    /**
     * Set the folders tab icon.
     * 
     * @param foldersIcon
     */
    public void setFoldersIcon(Icon foldersIcon) {
        if (expertMode) {
            //tabbedPane.setIconAt(FOLDERS_INDEX, foldersIcon);
        }
    }

    /**
     * @param tabIndex
     *            the select tab index
     */
    public void setActiveTab(int tabIndex) {
        if (expertMode) {
            tabbedPane.setSelectedIndex(tabIndex);
        }
    }

    /**
     * Set the computers tab icon.
     * 
     * @param computersIcon
     */
    public void setComputersIcon(Icon computersIcon) {
        if (showComputersTab && expertMode) {
            try {
                //tabbedPane.setIconAt(COMPUTERS_INDEX, computersIcon);
            } catch (Exception e) {
                // Ignore. This will fail on preference setting change,
                // just before restart.
            }
        }
    }

    /**
     * Listener to populate the folders and computers the first time the tabs
     * are selected.
     */
    private class MyChangelistener implements ChangeListener {

        private AtomicBoolean done = new AtomicBoolean();

        public void stateChanged(ChangeEvent e) {
            if (!done.getAndSet(true)) {
                foldersTab.populate();
                computersTab.populate();
            }
        }
    }
}
