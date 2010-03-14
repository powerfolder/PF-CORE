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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.computers.ComputersTab;
import de.dal33t.powerfolder.ui.folders.FoldersTab;
import de.dal33t.powerfolder.ui.status.HomeTab;
import de.dal33t.powerfolder.ui.start.WelcomeTab;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the main tabbed pain component in the PowerFolder GUI.
 */
public class MainTabbedPane extends PFUIComponent {

    public static final int WELCOME_INDEX = 0;
    public static final int HOME_INDEX = 1;
    public static final int FOLDERS_INDEX = 2;
    public static final int COMPUTERS_INDEX = 3;

    private WelcomeTab welcomeTab;
    private HomeTab homeTab;
    private FoldersTab foldersTab;
    private ComputersTab computersTab;

    /**
     * The main tabbed pane.
     */
    private JTabbedPane uiComponent;

    /**
     * Constructor. Creates the main tabbed pane.
     * 
     * @param controller
     */
    public MainTabbedPane(Controller controller) {
        super(controller);
    }

    /**
     * @return the ui main tabbed pane.
     */
    public JTabbedPane getUIComponent() {

        if (uiComponent == null) {
            // Initalize components
            initComponents();
            uiComponent.add(Translation
                .getTranslation("main_tabbed_pane.welcome.name"), welcomeTab
                .getUIComponent());

            uiComponent.add(Translation
                .getTranslation("main_tabbed_pane.home.name"), homeTab
                .getUIComponent());

            uiComponent.add(Translation
                .getTranslation("main_tabbed_pane.folders.name"), foldersTab
                .getUIComponent());

            if (getController().isBackupOnly()) {
                // Do not display computers tab in backup only mode, BUT
                // need to create it anyways to prevent UI events breaking.
                computersTab.getUIComponent();
            } else {
                uiComponent.add(Translation
                    .getTranslation("main_tabbed_pane.computers.name"),
                    computersTab.getUIComponent());
            }

            String key = Translation.getTranslation("main_tabbed_pane.welcome.key");
            uiComponent.setMnemonicAt(WELCOME_INDEX, (int) Character.toUpperCase(
                    key.charAt(0)));
            uiComponent.setToolTipTextAt(WELCOME_INDEX, Translation
                .getTranslation("main_tabbed_pane.welcome.description"));
            uiComponent.setIconAt(WELCOME_INDEX, Icons.getIconById(Icons.WELCOME));

            key = Translation.getTranslation("main_tabbed_pane.home.key");
            uiComponent.setMnemonicAt(HOME_INDEX, (int) Character.toUpperCase(key
                .charAt(0)));
            uiComponent.setToolTipTextAt(HOME_INDEX, Translation
                .getTranslation("main_tabbed_pane.home.description"));
            uiComponent.setIconAt(HOME_INDEX, Icons.getIconById(Icons.HOME));

            key = Translation.getTranslation("main_tabbed_pane.folders.key");
            uiComponent.setMnemonicAt(FOLDERS_INDEX, (int) Character
                .toUpperCase(key.charAt(0)));
            uiComponent.setToolTipTextAt(FOLDERS_INDEX, Translation
                .getTranslation("main_tabbed_pane.folders.description"));
            uiComponent.setIconAt(FOLDERS_INDEX, Icons.getIconById(Icons.FOLDER));

            if (!getController().isBackupOnly()) {
                key = Translation.getTranslation("main_tabbed_pane.computers.key");
                uiComponent.setMnemonicAt(COMPUTERS_INDEX, (int) Character
                    .toUpperCase(key.charAt(0)));
                uiComponent.setToolTipTextAt(COMPUTERS_INDEX, Translation
                    .getTranslation("main_tabbed_pane.computers.description"));
                uiComponent.setIconAt(COMPUTERS_INDEX, Icons
                    .getIconById(Icons.COMPUTER));
            }

            uiComponent.addChangeListener(new MyChagelistener());
        }

        return uiComponent;
    }

    public int getSelectedTabIndex() {
        return uiComponent.getSelectedIndex();
    }

    /**
     * Initialize the components of the pane.
     */
    private void initComponents() {
        uiComponent = new JTabbedPane();
        uiComponent.setOpaque(false);
        welcomeTab = new WelcomeTab(getController());
        homeTab = new HomeTab(getController());
        foldersTab = new FoldersTab(getController());
        computersTab = new ComputersTab(getController());
    }

    /**
     * Add a change listener to the main tabbed pane.
     * 
     * @param l
     */
    public void addTabbedPaneChangeListener(ChangeListener l) {
        getUIComponent().addChangeListener(l);
    }

    /**
     * Remove a change listener from the main tabbed pane.
     * 
     * @param l
     */
    public void removeTabbedPaneChangeListener(ChangeListener l) {
        getUIComponent().removeChangeListener(l);
    }

    /**
     * Set the home tab icon.
     * 
     * @param homeIcon
     */
    public void setHomeIcon(Icon homeIcon) {
        uiComponent.setIconAt(HOME_INDEX, homeIcon);
    }

    /**
     * Set the folders tab icon.
     * 
     * @param foldersIcon
     */
    public void setFoldersIcon(Icon foldersIcon) {
        uiComponent.setIconAt(FOLDERS_INDEX, foldersIcon);
    }

    /**
     * Set the computers tab icon.
     * 
     * @param computersIcon
     */
    public void setComputersIcon(Icon computersIcon) {
        if (!getController().isBackupOnly()) {
            try {
                uiComponent.setIconAt(COMPUTERS_INDEX, computersIcon);
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
    private class MyChagelistener implements ChangeListener {

        private AtomicBoolean done = new AtomicBoolean();

        public void stateChanged(ChangeEvent e) {
            if (!done.getAndSet(true)) {
                foldersTab.populate();
                computersTab.populate();
            }
        }
    }
}
