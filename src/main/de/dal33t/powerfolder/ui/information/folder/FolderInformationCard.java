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
 * $Id: InformationFilesCard.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder;

import java.awt.Image;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationCardType;
import de.dal33t.powerfolder.ui.information.folder.files.FilesTab;
import de.dal33t.powerfolder.ui.information.folder.members.MembersExpertTab;
import de.dal33t.powerfolder.ui.information.folder.members.MembersSimpleTab;
import de.dal33t.powerfolder.ui.information.folder.members.MembersTab;
import de.dal33t.powerfolder.ui.information.folder.problems.ProblemsTab;
import de.dal33t.powerfolder.ui.information.folder.settings.SettingsTab;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class FolderInformationCard extends InformationCard {

    private FolderInfo folderInfo;
    private JTabbedPane tabbedPane;
    private FilesTab filesTab;
    private MembersTab membersTab;
    private SettingsTab settingsTab;
    private ProblemsTab problemsTab;

    private final ProblemListener problemListener;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FolderInformationCard(Controller controller) {
        super(controller);

        if (ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController())) {
            filesTab = new FilesTab(getController());
        } else {
            filesTab = null;
        }

        if (ConfigurationEntry.MEMBERS_ENABLED.getValueBoolean(getController()))
        {
            if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
                membersTab = new MembersExpertTab(getController());
            } else {
                membersTab = new MembersSimpleTab(getController());
            }
        } else {
            membersTab = null;
        }

        if (ConfigurationEntry.SETTINGS_ENABLED
            .getValueBoolean(getController()))
        {
            settingsTab = new SettingsTab(getController());
        } else {
            settingsTab = null;
        }

        problemsTab = new ProblemsTab(getController());
        problemListener = new MyProblemListener();

        initialize();
        buildUIComponent();

        updateProblems();
    }

    public InformationCardType getInformationCardType() {
        return InformationCardType.FOLDER;
    }

    /**
     * Sets the folder in the tabs.
     * 
     * @param folderInfo
     */
    private void setFolderInfo0(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
        if (membersTab != null) {
            membersTab.setFolderInfo(folderInfo);
        }
        if (settingsTab != null) {
            settingsTab.setFolderInfo(folderInfo);
        }
        problemsTab.setFolderInfo(folderInfo);
    }

    /**
     * Sets the folder in the tabs.
     * 
     * @param folderInfo
     */
    public void setFileInfo(FileInfo fileInfo) {
        if (fileInfo.isDeleted()) {
            setFolderInfoDeleted(fileInfo.getFolderInfo());
        } else {
            setFolderInfo(fileInfo.getFolderInfo());
        }

        String fn = fileInfo.getFilenameOnly();
        String subDir = fileInfo.getRelativeName().replace(fn, "");
        if (StringUtils.isNotBlank(subDir) && filesTab != null) {
            filesTab.selectionChanged(subDir);
        }
    }

    /**
     * Sets the folder in the tabs.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        detachProblemListener();
        setFolderInfo0(folderInfo);
        if (filesTab != null) {
            filesTab.setFolderInfo(folderInfo);
        }
        atachProblemListener();
        updateProblems();
    }

    /**
     * Sets the folder in the tabs with new set and sort date descending.
     * 
     * @param folderInfo
     */
    public void setFolderInfoLatest(FolderInfo folderInfo) {
        detachProblemListener();
        setFolderInfo0(folderInfo);
        if (filesTab != null) {
            filesTab.setFolderInfoLatest(folderInfo);
        }
        atachProblemListener();
        updateProblems();
    }

    public void setFolderInfoDeleted(FolderInfo folderInfo) {
        detachProblemListener();
        setFolderInfo0(folderInfo);
        if (filesTab != null) {
            filesTab.setFolderInfoDeleted(folderInfo);
        }
        atachProblemListener();
        updateProblems();
    }

    public void setFolderInfoUnsynced(FolderInfo folderInfo) {
        detachProblemListener();
        setFolderInfo0(folderInfo);
        if (filesTab != null) {
            filesTab.setFolderInfoUnsynced(folderInfo);
        }
        atachProblemListener();
        updateProblems();
    }

    private void detachProblemListener() {
        if (folderInfo != null) {
            Folder folder = getController().getFolderRepository().getFolder(
                folderInfo);
            if (folder != null) {
                folder.removeProblemListener(problemListener);
            }
        }
    }

    private void atachProblemListener() {
        getController().getFolderRepository().getFolder(folderInfo)
            .addProblemListener(problemListener);
    }

    /**
     * Control the folder's problems from here so that the tab can be removed if
     * there are no problems.
     */
    private void updateProblems() {
        if (folderInfo == null) {
            // No fi, no show.
            removeProblemsTab();
        } else {
            List<Problem> problemList = getController().getFolderRepository()
                .getFolder(folderInfo).getProblems();
            if (problemList.isEmpty()) {
                removeProblemsTab();
            } else {
                if (tabbedPane.getComponentCount() <= getProblemsTabIndex()) {
                    addProblemsTab();
                }
            }
            problemsTab.updateProblems(problemList);
        }
    }

    /**
     * Add the problems tab to the pane.
     */
    private void addProblemsTab() {
        tabbedPane.addTab(Translation
            .getTranslation("folder_information_card.problems.title"),
            problemsTab.getUIComponent());
        // tabbedPane.setIconAt(getProblemsTabIndex(), Icons
        // .getIconById(Icons.PROBLEMS));
        tabbedPane
            .setToolTipTextAt(getProblemsTabIndex(), Translation
                .getTranslation("folder_information_card.problems.tips"));
    }

    /**
     * Remove the problems tab if displayed.
     */
    private void removeProblemsTab() {
        if (tabbedPane.getComponentCount() >= 1 + getProblemsTabIndex()) {
            tabbedPane.remove(getProblemsTabIndex());
        }
    }

    /**
     * Gets the image for the card.
     * 
     * @return
     */
    public Image getCardImage() {
        return Icons.getImageById(Icons.FOLDER);
    }

    /**
     * Gets the title for the card.
     * 
     * @return
     */
    public String getCardTitle() {
        return folderInfo.getLocalizedName();
    }

    /**
     * Gets the ui component after initializing and building if necessary
     * 
     * @return
     */
    public JComponent getUIComponent() {
        return tabbedPane;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        tabbedPane = new JTabbedPane();
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
        if (filesTab != null) {
            tabbedPane.addTab(Translation
                .getTranslation("folder_information_card.files.title"),
                filesTab.getUIComponent());
            tabbedPane.setToolTipTextAt(getFilesTabIndex(), Translation
                .getTranslation("folder_information_card.files.tips"));
        }

        if (membersTab != null) {
            tabbedPane.addTab(Translation
                .getTranslation("folder_information_card.members.title"),
                membersTab.getUIComponent());
            tabbedPane.setToolTipTextAt(getMembersTabIndex(), Translation
                .getTranslation("folder_information_card.members.tips"));
        }

        if (settingsTab != null) {
            JScrollPane scrollPane = new JScrollPane(
                settingsTab.getUIComponent(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            UIUtil.removeBorder(scrollPane);
            tabbedPane.addTab(Translation
                .getTranslation("folder_information_card.settings.title"),
                scrollPane);
            tabbedPane.setToolTipTextAt(getSettingsTabIndex(), Translation
                .getTranslation("folder_information_card.settings.tips"));
        }
    }

    /**
     * Display the files tab.
     */
    public void showFiles() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(getFilesTabIndex());
    }

    /**
     * Display the members tab.
     */
    public void showMembers() {
        if (getController().isBackupOnly()) {
            logSevere("Called showMembers() for a backup only client ?!");
        } else {
            ((JTabbedPane) getUIComponent())
                .setSelectedIndex(getMembersTabIndex());
        }
    }

    /**
     * Display the settings tab.
     */
    public void showSettings() {
        ((JTabbedPane) getUIComponent())
            .setSelectedIndex(getSettingsTabIndex());
    }

    /**
     * Fires the move local folder function on the settings tab.
     */
    public void moveLocalFolder() {
        settingsTab.moveLocalFolder();
    }

    /**
     * Display the problems tab.
     */
    public void showProblems() {
        ((JTabbedPane) getUIComponent())
            .setSelectedIndex(getProblemsTabIndex());
    }

    /**
     * Files tab is tab index zero.
     * 
     * @return
     */
    private static int getFilesTabIndex() {
        return 0;
    }

    /**
     * Members tab is tab index 1 - if tab enabled.
     * 
     * @return
     */
    private static int getMembersTabIndex() {
        return 1;
    }

    /**
     * Settings tab is tab index 2, or 1 if members tab not enabled.
     * 
     * @return
     */
    private int getSettingsTabIndex() {
        int maxIndex = 2;
        if (!ConfigurationEntry.MEMBERS_ENABLED
            .getValueBoolean(getController()))
        {
            maxIndex -= 1;
        }
        if (!ConfigurationEntry.SETTINGS_ENABLED
            .getValueBoolean(getController()))
        {
            maxIndex -= 1;
        }
        return getController().isBackupOnly() ? 1 : maxIndex;
    }

    /**
     * Problems tab is tab index 3, or 2 if members tab not enabled.
     * 
     * @return
     */
    private int getProblemsTabIndex() {
        int maxIndex = 3;
        if (!ConfigurationEntry.FILES_ENABLED.getValueBoolean(getController()))
        {
            maxIndex -= 1;
        }
        if (!ConfigurationEntry.MEMBERS_ENABLED
            .getValueBoolean(getController()))
        {
            maxIndex -= 1;
        }
        if (!ConfigurationEntry.SETTINGS_ENABLED
            .getValueBoolean(getController()))
        {
            maxIndex -= 1;
        }
        return maxIndex;
    }

    private class MyProblemListener implements ProblemListener {
        public void problemAdded(Problem problem) {
            updateProblems();
        }

        public void problemRemoved(Problem problem) {
            updateProblems();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}