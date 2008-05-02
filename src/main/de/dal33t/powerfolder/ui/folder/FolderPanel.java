package de.dal33t.powerfolder.ui.folder;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.chat.FolderChatPanel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Displays the folder contents. Holds the 5 tabs: Home, Files, members, chat
 * and Settings
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class FolderPanel extends PFUIPanel {
    public static final int HOME_TAB = 0;
    public static final int FILES_TAB = 1;
    //public static final int NEW_FILES_TAB = 2;
    public static final int MEMBERS_TAB = 2;
    public static final int CHAT_TAB = 3;
    public static final int SETTINGS_TAB = 4;

    private final boolean previewMode;

    private JTabbedPane tabbedPanel;
    private Folder folder;
    private HomeTab homeTab;
    private FilesTab filesTab;
    //private NewFilesTab newFilesTab;
    private MembersTab membersTab;
    private FolderChatPanel folderChatPanel;
    private SettingsTab settingsTab;
    private final AtomicBoolean settingsTabSet = new AtomicBoolean();

    public FolderPanel(Controller controller, boolean previewMode) {
        super(controller);
        this.previewMode = previewMode;
    }

    /**
     * Set the Folder this panel should display.
     * 
     * @param folder
     *            the folder this panel should display
     */
    public void setFolder(Folder folder) {        
        filesTab.setDirectory(folder.getDirectory());
        //newFilesTab.setDirectory(folder.getDirectory());
        setFolder0(folder);
    }

    private void setFolder0(Folder folder) {
        this.folder = folder;
        membersTab.setFolder(folder);
        folderChatPanel.setFolder(folder);
        homeTab.setFolder(folder);
        tabbedPanel.setIconAt(HOME_TAB, Icons.FOLDER);

        // Do not show settings tab in preview
        if (folder.isPreviewOnly()) {
            synchronized (settingsTabSet) {
                if (settingsTabSet.get()) {
                    tabbedPanel.remove(SETTINGS_TAB);
                    settingsTabSet.set(false);
                }
            }
        } else {
            synchronized (settingsTabSet) {
                JComponent settingsComponent = settingsTab.getUIComponent();
                settingsTab.setFolder(folder);
                if (!settingsTabSet.get()) {
                    tabbedPanel.add(' ' + settingsTab.getTitle() + ' ',
                            settingsComponent);
                    tabbedPanel.setMnemonicAt(SETTINGS_TAB,
                            Translation.getTranslation("folderpanel.settings.key").charAt(0));
                    tabbedPanel.setIconAt(SETTINGS_TAB, Icons.SETTINGS);
                    settingsTabSet.set(true);
                }
            }
        }
    }

    /**
     * The (sub) Directory of a Folder this panel should display
     * 
     * @param directory
     *            The Directory to Display
     */
    public void setDirectory(Directory directory) {
        setFolder0(directory.getRootFolder());
        filesTab.setDirectory(directory);
        //newFilesTab.setDirectory(directory);
        tabbedPanel.setSelectedIndex(FILES_TAB);
    }

    /**
     * @return the folder chat panel
     */
    public FolderChatPanel getChatPanel() {
        return folderChatPanel;
    }

    public JComponent getUIComponent() {
        if (tabbedPanel == null) {
            initComponents();
        }
        return tabbedPanel;
    }

    /**
     *
     * @param tab ta to show
     */
    public void setTab(int tab) {
        if (tab == CHAT_TAB || tab == HOME_TAB || tab == FILES_TAB //|| tab == NEW_FILES_TAB
            || tab == MEMBERS_TAB || tab == SETTINGS_TAB ) //|| tab == PROBLEMS_TAB)
        {
            tabbedPanel.setSelectedIndex(tab);
        } else {
            throw new IllegalArgumentException("not a valid tab: " + tab);
        }
    }

    private FolderTab getCurrentTab() {
        switch (tabbedPanel.getSelectedIndex()) {
            case FILES_TAB :
                return filesTab;
            //case NEW_FILES_TAB :
                //return newFilesTab;
            case HOME_TAB :
                return homeTab;
            case MEMBERS_TAB :
                return membersTab;
            case CHAT_TAB :
                return folderChatPanel;
            case SETTINGS_TAB :
                return settingsTab;
            //case PROBLEMS_TAB :
            //    return problemsTab;
            default :
                throw new IllegalStateException("invalid tab:"
                    + tabbedPanel.getSelectedIndex());
        }

    }

    /**
     * returns the title the InformationQarter should display It uses the
     * selected Tab and the folder name in the title.
     *
     * @return the title
     */
    public String getTitle() {
        if (folder != null) {
            return Translation.getTranslation("title.my.folders") + " > "
                    + folder.getName() + " > " + getCurrentTab().getTitle();
        }
        return "";
    }

    private void initComponents() {
        tabbedPanel = new JTabbedPane();
        filesTab = new FilesTab(getController(), this);
        //newFilesTab = new NewFilesTab(getController(), this);
        membersTab = new MembersTab(getController());
        folderChatPanel = new FolderChatPanel(getController(),
            getUIController().getChatModel());
        homeTab = new HomeTab(getController(), previewMode);
        settingsTab = new SettingsTab(getController(), previewMode);
        
        tabbedPanel.add(' '
                + homeTab.getTitle() + ' ',
            homeTab.getUIComponent());

        tabbedPanel.setMnemonicAt(HOME_TAB,
                Translation.getTranslation("folderpanel.home.key").charAt(0));

        tabbedPanel.add(
                ' ' + filesTab.getTitle() + ' ', filesTab
                .getUIComponent());
        tabbedPanel.setMnemonicAt(FILES_TAB,
                Translation.getTranslation("folderpanel.files.key").charAt(0));
        tabbedPanel.setIconAt(FILES_TAB, Icons.DIRECTORY);

        //tabbedPanel.add(
        //        ' ' + newFilesTab.getTitle() + ' ', newFilesTab
        //        .getUIComponent());
        //tabbedPanel.setMnemonicAt(NEW_FILES_TAB,
        //        Translation.getTranslation("folderpanel.new_files.key").charAt(0));
        //tabbedPanel.setIconAt(NEW_FILES_TAB, Icons.DIRECTORY_NEW);

        tabbedPanel.add(' '
                + membersTab.getTitle() + ' ',
            membersTab.getUIComponent());
        tabbedPanel.setMnemonicAt(MEMBERS_TAB,
                Translation.getTranslation("folderpanel.members.key").charAt(0));
        tabbedPanel.setIconAt(MEMBERS_TAB, Icons.NODE_FRIEND_CONNECTED);

        tabbedPanel.add(
                ' ' + folderChatPanel.getTitle() + ' ',
            folderChatPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(CHAT_TAB,
                Translation.getTranslation("folderpanel.chat.key").charAt(0));
        tabbedPanel.setIconAt(CHAT_TAB, Icons.CHAT);

        UIUtil.removeBorder(tabbedPanel);

        tabbedPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                getUIController().getInformationQuarter().setTitle(getTitle());
            }
        });
    }

    public FilesTab getFilesTab() {
        return filesTab;
    }

    //public NewFilesTab getNewFilesTab() {
    //    return newFilesTab;
    //}

    /**
     * Called by the FilesTab to add new ignore patterns to the Settings tab.
     * Patterns are separated by new line characters.
     *
     * @param pattern
     */
    public void addPatterns(String patterns) {
        setTab(SETTINGS_TAB);
        settingsTab.showAddPane(patterns);
    }

    /**
     * Called by the FilesTab to remove an ignore pattern from the Settings tab.
     * Patterns are separated by new line characters.
     *
     * @param pattern
     */
    public void removePatterns(String patterns) {
        setTab(SETTINGS_TAB);
        settingsTab.removePatterns(patterns);
    }

    /**
     * Toggles the Details pane (only if Files is selected).
     */
    public void toggleDetails() {
        if (getCurrentTab().equals(filesTab)) {
            filesTab.toggeDetails();
        //} else if (getCurrentTab().equals(newFilesTab)) {
        //    newFilesTab.toggeDetails();
        }
    }
}
