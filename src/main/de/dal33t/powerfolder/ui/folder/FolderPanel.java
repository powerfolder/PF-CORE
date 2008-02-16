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
    public static final int MEMBERS_TAB = 2;
    public static final int CHAT_TAB = 3;
    public static final int SETTINGS_TAB = 4;

    private JTabbedPane tabbedPanel;
    private Folder folder;
    private HomeTab homeTab;
    private FilesTab filesTab;
    private MembersTab membersTab;
    private FolderChatPanel folderChatPanel;
    private SettingsTab settingsTab;

    public FolderPanel(Controller controller) {
        super(controller);
    }

    /**
     * Set the Folder this panel should display.
     * 
     * @param folder
     *            the folder this panel should display
     */
    public void setFolder(Folder folder) {        
        filesTab.setDirectory(folder.getDirectory());
        setFolder0(folder);
    }

    private void setFolder0(Folder folder) {
        this.folder = folder;
        membersTab.setFolder(folder);
        folderChatPanel.setFolder(folder);
        homeTab.setFolder(folder);
        settingsTab.setFolder(folder);
        tabbedPanel.setIconAt(HOME_TAB, Icons.FOLDER);
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
        if (tab == CHAT_TAB || tab == HOME_TAB || tab == FILES_TAB
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
            String start = folder.isPreviewOnly() ?
                    Translation.getTranslation("title.preview.folders") :
                    Translation.getTranslation("title.my.folders");
            return start + " > " + folder.getName() + " > " + getCurrentTab().getTitle();
        }
        return "";
    }

    private void initComponents() {
        tabbedPanel = new JTabbedPane();
        filesTab = new FilesTab(getController(), this);
        membersTab = new MembersTab(getController());
        folderChatPanel = new FolderChatPanel(getController(),
            getUIController().getChatModel());
        homeTab = new HomeTab(getController());
        settingsTab = new SettingsTab(getController());
        //problemsTab = new ProblemsTab(getController());
        
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

        tabbedPanel.add(
                ' ' + settingsTab.getTitle()
                + ' ', settingsTab.getUIComponent());
        tabbedPanel.setMnemonicAt(SETTINGS_TAB,
                Translation.getTranslation("folderpanel.settings.key").charAt(0));
        tabbedPanel.setIconAt(SETTINGS_TAB, Icons.SETTINGS);

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

    /**
     * Called by the FilesTab to add a new ignore pattern to the Settings tab.
     *
     * @param pattern
     */
    public void addPattern(String pattern) {
        setTab(SETTINGS_TAB);
        settingsTab.showAddPane(pattern);
    }

    /**
     * Called by the FilesTab to remove an ignore pattern from the Settings tab.
     *
     * @param pattern
     */
    public void removePatternsForFile(String fileName) {
        setTab(SETTINGS_TAB);
        settingsTab.removePatternsForFile(fileName);
    }

    /**
     * Toggles the Details pane (only if Files is selected).
     */
    public void toggleDetails() {
        if (getCurrentTab().equals(filesTab)) {
            filesTab.toggeDetails();
        }
    }
}
