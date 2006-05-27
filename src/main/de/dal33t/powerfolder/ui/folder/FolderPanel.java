package de.dal33t.powerfolder.ui.folder;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.chat.FolderChatPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

/**
 * Displays the folder contents. Holds the 4 tabs: Home, Files, members, and chat
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class FolderPanel {
    public static final int HOME_TAB = 0;
    public static final int FILES_TAB = 1;    
    public static final int MEMBERS_TAB = 2;
    public static final int CHAT_TAB = 3;
    
    private JTabbedPane tabbedPanel;
    private Folder folder;
    private Controller controller;
    private DirectoryPanel directoryPanel;
    private FolderMembersPanel membersPanel;
    private FolderChatPanel folderChatPanel;
    
    private FolderHomeTabPanel folderHomeTabPanel;

    public FolderPanel(Controller controller) {
        this.controller = controller;
    }

    /**
     * Set the Folder this panel should display.
     * 
     * @param folder
     *            the folder this panel should display
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
        directoryPanel.setDirectory(folder.getDirectory());
        setFolder0(folder);
    }

    
    private void setFolder0(Folder folder) {
        membersPanel.setFolder(folder);
        folderChatPanel.chatAbout(folder);
        folderHomeTabPanel.setFolder(folder);
        tabbedPanel.setIconAt(HOME_TAB, Icons.getIconFor(folder.getInfo()));
    }
    
    /**
     * The (sub) Directory of a Folder this panel should display
     * 
     * @param directory
     *            The Directory to Display
     */
    public void setDirectory(Directory directory) {        
        setFolder0(directory.getRootFolder());        
        directoryPanel.setDirectory(directory);
        tabbedPanel.setSelectedIndex(FILES_TAB);
    }

    public JComponent getUIComponent() {
        if (tabbedPanel == null) {
            initComponents();
        }
        return tabbedPanel;
    }
    
    /** package protected */
    public void setTab(int tab) {
        if (tab == CHAT_TAB || tab == HOME_TAB || tab == FILES_TAB || tab == MEMBERS_TAB) {
            tabbedPanel.setSelectedIndex(tab);
        } else {
            throw new IllegalArgumentException("not a valid tab: " + tab);
        } 
    }

    /**
     * returns the title the InformationQarter should display It uses the
     * selected Tab and the folder name in the title.
     */
    public String getTitle() {
        if (folder != null) {
            String text = "";
            switch (tabbedPanel.getSelectedIndex()) {
                case FILES_TAB : {
                    text = Translation.getTranslation("general.files");
                    break;
                }
                case HOME_TAB : { 
                    text = Translation.getTranslation("folderpanel.hometab.title");
                    break;
                }
                case MEMBERS_TAB : {
                    text = Translation.getTranslation("myfolderstable.members");
                    break;
                }
                case CHAT_TAB : {
                    text = Translation.getTranslation("openchat.name");
                    break;
                }
            }
            return Translation.getTranslation("title.my.folders") + " > "
                + folder.getName() + " > " + text;
        }
        return "";
    }

    private void initComponents() {
        tabbedPanel = new JTabbedPane();
        directoryPanel = new DirectoryPanel(controller);
        membersPanel = new FolderMembersPanel(controller);
        folderChatPanel = new FolderChatPanel(controller); 
        folderHomeTabPanel = new FolderHomeTabPanel(controller);
                
        tabbedPanel.add(
            " " + Translation.getTranslation("folderpanel.hometab.title") + " ",
            folderHomeTabPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(HOME_TAB, KeyEvent.VK_F);
        
        
        tabbedPanel.add(
            " " + Translation.getTranslation("general.files") + " ",
            directoryPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(FILES_TAB, KeyEvent.VK_F);
        tabbedPanel.setIconAt(FILES_TAB, Icons.DIRECTORY);
        
        tabbedPanel.add(" "
            + Translation.getTranslation("myfolderstable.members") + " ",
            membersPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(MEMBERS_TAB, KeyEvent.VK_M);
        tabbedPanel.setIconAt(MEMBERS_TAB, Icons.NODE);
        
        tabbedPanel.add(
            " " + Translation.getTranslation("openchat.name") + " ", folderChatPanel
                .getUIComponent());
        tabbedPanel.setMnemonicAt(CHAT_TAB, KeyEvent.VK_C);
        tabbedPanel.setIconAt(CHAT_TAB, Icons.CHAT);
        
        Util.removeBorder(tabbedPanel);

        tabbedPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                controller.getUIController().getInformationQuarter().setTitle(
                    getTitle());
            }
        });
    }

    public DirectoryPanel getDirectoryPanel() {
        return directoryPanel;
    }
}
