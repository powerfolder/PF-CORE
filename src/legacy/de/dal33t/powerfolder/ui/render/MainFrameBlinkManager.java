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
package de.dal33t.powerfolder.ui.render;

import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.MainTabbedPane;
import de.dal33t.powerfolder.ui.MainFrame;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Manages the blinking of tab icons in main tabs. Flash every second if tab not
 * showing and (message, invitation, friendship or warning detected).
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class MainFrameBlinkManager extends PFUIComponent {

    private final AtomicBoolean flashStatusTab = new AtomicBoolean();
    private final AtomicBoolean flashFolderTab = new AtomicBoolean();
    private final AtomicBoolean flashMemberTab = new AtomicBoolean();
    private final AtomicInteger selectedMainTab = new AtomicInteger();

    private final UIController uiController;

    /**
     * Create a Blink Manager for the mainframe tabs
     * 
     * @param uiController
     *            the UIController
     */
    public MainFrameBlinkManager(UIController uiController) {
        super(uiController.getController());
        this.uiController = uiController;
        MyTimerTask task = new MyTimerTask();
        getController().scheduleAndRepeat(task, 1000);
        uiController.getApplicationModel().getNoticesModel()
            .getUnreadNoticesCountVM()
            .addValueChangeListener(new MyNoticesCountListener());
        uiController.getMainFrame().addTabbedPaneChangeListener(
            new MyMainTabChangeListener());
        uiController.getController().getFolderRepository()
            .addProblemListenerToAllFolders(new MyProblemListener());
    }

    /**
     * Update the tabs with the image required.
     */
    private void update() {

        boolean blink = System.currentTimeMillis() / 1000 % 2 != 0;

        MainFrame mainFrame = uiController.getMainFrame();
        if (blink && flashStatusTab.get()) {
            mainFrame.setStatusTabIcon(Icons.getIconById(Icons.BLANK));
        } else {
            mainFrame.setStatusTabIcon(Icons.getIconById(Icons.STATUS));
        }

        if (blink && flashFolderTab.get()) {
            mainFrame.setFoldersTabIcon(Icons.getIconById(Icons.BLANK));
        } else {
            mainFrame.setFoldersTabIcon(Icons.getIconById(Icons.FOLDER));
        }

        if (blink && flashMemberTab.get()) {
            mainFrame.setComputersTabIcon(Icons.getIconById(Icons.BLANK));
        } else {
            mainFrame.setComputersTabIcon(Icons.getIconById(Icons.COMPUTER));
        }
    }

    /**
     * Sets the status icon flashing.
     * 
     * @param flash
     */
    private void flashStatusTabIcon(boolean flash) {
        flashStatusTab.set(flash);
    }

    /**
     * Sets the folder icon flashing.
     * 
     * @param flash
     */
    private void flashFolderTabIcon(boolean flash) {
        flashFolderTab.set(flash);
    }

    /**
     * Sets the member icon flashing.
     * 
     * @param flash
     */
    private void flashMemberTabIcon(boolean flash) {
        flashMemberTab.set(flash);
    }

    /* ------------- */
    /* Inner Classes */
    /* ------------- */

    /**
     * Timer task, always running, updates the tab icons.
     */
    private class MyTimerTask extends TimerTask {
        public void run() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    update();
                }
            });
        }
    }

    /**
     * Listen for incoming invitations.
     */
    private class MyNoticesCountListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {

            Integer count = (Integer) evt.getNewValue();

            if (count == null || count == 0
                || selectedMainTab.get() == MainTabbedPane.STATUS_INDEX)
            {
                return;
            }

            flashStatusTabIcon(true);
            update();
        }
    }

    /**
     * Listen to added folder problems.
     */
    private class MyProblemListener implements ProblemListener {

        public void problemAdded(Problem problem) {
            if (selectedMainTab.get() == MainTabbedPane.FOLDERS_INDEX) {
                return;
            }

            flashFolderTabIcon(true);
            update();
        }

        public void problemRemoved(Problem problem) {
            // Do nothing
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Listen to main tab selection changes.
     */
    private class MyMainTabChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {

            selectedMainTab.set(uiController.getMainFrame()
                .getSelectedMainTabIndex());

            if (selectedMainTab.get() == MainTabbedPane.STATUS_INDEX) {
                flashStatusTabIcon(false);
                update();
            } else if (selectedMainTab.get() == MainTabbedPane.FOLDERS_INDEX) {
                flashFolderTabIcon(false);
                update();
            } else if (selectedMainTab.get() == MainTabbedPane.COMPUTERS_INDEX)
            {
                flashMemberTabIcon(false);
                update();
            }
        }
    }
}