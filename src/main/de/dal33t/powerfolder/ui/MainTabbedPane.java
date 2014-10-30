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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.computers.ComputersTab;
import de.dal33t.powerfolder.ui.folders.FoldersTab;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * This is the main tabbed pain component in the PowerFolder GUI. In expert
 * mode, it shows the status, folders and computers tab. In non-expert mode, it
 * just returns the FolderTab.
 */
public class MainTabbedPane extends PFUIComponent {

    public static final int FOLDERS_INDEX = 0;
    public static final int COMPUTERS_INDEX = 1;

    private FoldersTab foldersTab;
    private ComputersTab computersTab;

    private JTabbedPane tabbedPane;

    private final boolean showComputersTab;
    private final boolean showDeviceTab;
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
        showDeviceTab = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController())
            && PreferencesEntry.SHOW_DEVICES.getValueBoolean(getController());
    }

    /**
     * @return the ui main tabbed pane.
     */
    public JComponent getUIComponent() {

        if (!initialized.getAndSet(true)) {
            // Initalize components
            initComponents();

            if (showDeviceTab) {

                tabbedPane.add(Translation.getTranslation(
                        "main_tabbed_pane.folders.name"),
                        foldersTab.getUIComponent());

                if (showComputersTab) {
                    tabbedPane.add(Translation
                        .getTranslation("exp.main_tabbed_pane.computers.name"),
                        computersTab.getUIComponent());
                } else {
                    // Do not display computers tab in backup only mode, BUT
                    // need to create it anyways to prevent UI events breaking.
                    computersTab.getUIComponent();
                }

                String key = Translation
                    .getTranslation("main_tabbed_pane.folders.key");
                tabbedPane.setMnemonicAt(FOLDERS_INDEX,
                    (int) Character.toUpperCase(key.charAt(0)));
                tabbedPane.setToolTipTextAt(FOLDERS_INDEX, Translation
                    .getTranslation("main_tabbed_pane.folders.description"));

                if (showComputersTab) {
                    key = Translation
                        .getTranslation("exp.main_tabbed_pane.computers.key");
                    tabbedPane.setMnemonicAt(COMPUTERS_INDEX,
                        (int) Character.toUpperCase(key.charAt(0)));
                    tabbedPane.setToolTipTextAt(COMPUTERS_INDEX,
                        Translation.getTranslation(""
                            + "exp.main_tabbed_pane.computers.description"));
                }

                tabbedPane.addChangeListener(new MyChangelistener());

                CursorUtils.setHandCursor(tabbedPane);
                CursorUtils.setDefaultCursor(foldersTab.getUIComponent());
                CursorUtils.setDefaultCursor(computersTab.getUIComponent());

                setActiveTab(FOLDERS_INDEX);
            }
            foldersTab.populate();
        }

        if (showDeviceTab) {
            return tabbedPane;
        } else {
            return foldersTab.getUIComponent();
        }
    }

    public int getSelectedTabIndex() {
        if (showDeviceTab) {
            return tabbedPane.getSelectedIndex();
        } else {
            // Why is someone asking for the tab index,
            // when only the folder tab is showing?
            throw new IllegalStateException("Expert mode == " + showDeviceTab);
        }
    }

    /**
     * Initialize the components of the pane.
     */
    private void initComponents() {
        foldersTab = new FoldersTab(getController());
        if (showDeviceTab) {
            tabbedPane = new JTabbedPane();
            tabbedPane.setOpaque(false);
            computersTab = new ComputersTab(getController());
        }
    }

    public FoldersTab getFoldersTab() {
        return foldersTab;
    }

    /**
     * Add a change listener to the main tabbed pane.
     *
     * @param l
     */
    public void addTabbedPaneChangeListener(ChangeListener l) {
        if (showDeviceTab) {
            tabbedPane.addChangeListener(l);
        }
    }

    /**
     * Remove a change listener from the main tabbed pane.
     *
     * @param l
     */
    public void removeTabbedPaneChangeListener(ChangeListener l) {
        if (showDeviceTab) {
            tabbedPane.removeChangeListener(l);
        }
    }

    /**
     * @param tabIndex
     *            the select tab index
     */
    public void setActiveTab(int tabIndex) {
        if (showDeviceTab) {
            tabbedPane.setSelectedIndex(tabIndex);
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
