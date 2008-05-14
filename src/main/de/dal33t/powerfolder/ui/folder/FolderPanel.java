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

    private final int homeTabId;
    private final int filesTabId;
    private final int newFilesTabId;
    private final int membersTabId;
    private final int chatTabId;
    private final int settingsTabId;

    private final boolean previewMode;

    private JTabbedPane tabbedPanel;
    private Folder folder;
    private HomeTab homeTab;
    private FilesTab filesTab;
    private NewFilesTab newFilesTab;
    private MembersTab membersTab;
    private FolderChatPanel folderChatPanel;
    private SettingsTab settingsTab;
    private final AtomicBoolean settingsTabSet = new AtomicBoolean();

    public FolderPanel(Controller controller, boolean previewMode) {
        super(controller);
        this.previewMode = previewMode;
        if (previewMode) {
            homeTabId = 0;
            filesTabId = 1;
            newFilesTabId = -1;
            membersTabId = 2;
            chatTabId = 3;
            settingsTabId = 4;
        } else {
            homeTabId = 0;
            filesTabId = 1;
            newFilesTabId = 2;
            membersTabId = 3;
            chatTabId = 4;
            settingsTabId = 5;
        }
    }

    /**
     * Set the Folder this panel should display.
     * 
     * @param folder
     *            the folder this panel should display
     */
    public void setFolder(Folder folder) {        
        filesTab.setDirectory(folder.getDirectory());
        newFilesTab.setFolder(folder);
        setFolder0(folder);
    }

    private void setFolder0(Folder folder) {
        this.folder = folder;
        membersTab.setFolder(folder);
        folderChatPanel.setFolder(folder);
        homeTab.setFolder(folder);
        tabbedPanel.setIconAt(homeTabId, Icons.FOLDER);

        // Do not show settings tab in preview
        if (folder.isPreviewOnly()) {
            synchronized (settingsTabSet) {
                if (settingsTabSet.get()) {
                    tabbedPanel.remove(settingsTabId);
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
                    tabbedPanel.setMnemonicAt(settingsTabId,
                            Translation.getTranslation("folderpanel.settings.key").charAt(0));
                    tabbedPanel.setIconAt(settingsTabId, Icons.SETTINGS);
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
        tabbedPanel.setSelectedIndex(filesTabId);
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

    public int getChatTabId() {
        return chatTabId;
    }

    public int getFilesTabId() {
        return filesTabId;
    }

    public int getHomeTabId() {
        return homeTabId;
    }

    public int getMembersTabId() {
        return membersTabId;
    }

    public int getNewFilesTabId() {
        return newFilesTabId;
    }

    public int getSettingsTabId() {
        return settingsTabId;
    }

    /**
     *
     * @param tab tab to show
     */
    public void setTab(int tab) {
        if (tab == chatTabId || tab == homeTabId || tab == filesTabId || tab == newFilesTabId
            || tab == membersTabId || tab == settingsTabId)
        {
            tabbedPanel.setSelectedIndex(tab);
        } else {
            throw new IllegalArgumentException("not a valid tab: " + tab);
        }
    }

    private FolderTab getCurrentTab() {
        int tab = tabbedPanel.getSelectedIndex();
        if (tab == filesTabId) {
            return filesTab;
        } else if (tab == newFilesTabId) {
            return newFilesTab;
        } else if (tab == homeTabId) {
            return homeTab;
        } else if (tab == membersTabId) {
            return membersTab;
        } else if (tab == chatTabId) {
            return folderChatPanel;
        } else if (tab == settingsTabId) {
            return settingsTab;
        } else {
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
        newFilesTab = new NewFilesTab(getController());
        membersTab = new MembersTab(getController());
        folderChatPanel = new FolderChatPanel(getController(),
            getUIController().getChatModel());
        homeTab = new HomeTab(getController(), previewMode);
        settingsTab = new SettingsTab(getController(), previewMode, this);
        
        tabbedPanel.add(' '
                + homeTab.getTitle() + ' ',
            homeTab.getUIComponent());

        tabbedPanel.setMnemonicAt(homeTabId,
                Translation.getTranslation("folderpanel.home.key").charAt(0));

        tabbedPanel.add(
                ' ' + filesTab.getTitle() + ' ', filesTab
                .getUIComponent());
        tabbedPanel.setMnemonicAt(filesTabId,
                Translation.getTranslation("folderpanel.files.key").charAt(0));
        tabbedPanel.setIconAt(filesTabId, Icons.DIRECTORY);

        if (!previewMode) {
            tabbedPanel.add(
                    ' ' + newFilesTab.getTitle() + ' ', newFilesTab
                    .getUIComponent());
            tabbedPanel.setMnemonicAt(newFilesTabId,
                    Translation.getTranslation("folderpanel.new_files.key").charAt(0));
            tabbedPanel.setIconAt(newFilesTabId, Icons.DIRECTORY_NEW);
            if (!previewMode) {
                tabbedPanel.getComponentAt(newFilesTabId).setVisible(false);
            }
        }
        
        tabbedPanel.add(' '
                + membersTab.getTitle() + ' ',
            membersTab.getUIComponent());
        tabbedPanel.setMnemonicAt(membersTabId,
                Translation.getTranslation("folderpanel.members.key").charAt(0));
        tabbedPanel.setIconAt(membersTabId, Icons.NODE_FRIEND_CONNECTED);

        tabbedPanel.add(
                ' ' + folderChatPanel.getTitle() + ' ',
            folderChatPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(chatTabId,
                Translation.getTranslation("folderpanel.chat.key").charAt(0));
        tabbedPanel.setIconAt(chatTabId, Icons.CHAT);

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

    public NewFilesTab getNewFilesTab() {
        return newFilesTab;
    }

    /**
     * Called by the FilesTab to add new ignore patterns to the Settings tab.
     * Patterns are separated by new line characters.
     *
     * @param pattern
     */
    public void addPatterns(String patterns) {
        setTab(settingsTabId);
        settingsTab.showAddPane(patterns);
    }

    /**
     * Called by the FilesTab to remove an ignore pattern from the Settings tab.
     * Patterns are separated by new line characters.
     *
     * @param pattern
     */
    public void removePatterns(String patterns) {
        setTab(settingsTabId);
        settingsTab.removePatterns(patterns);
    }

    /**
     * Toggles the Details pane (only if Files is selected).
     */
    public void toggleDetails() {
        if (getCurrentTab().equals(filesTab)) {
            filesTab.toggeDetails();
        } else if (getCurrentTab().equals(newFilesTab)) {
            newFilesTab.toggeDetails();
        }
    }
}
