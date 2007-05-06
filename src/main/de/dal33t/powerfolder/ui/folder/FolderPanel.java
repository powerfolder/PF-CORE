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
   // public static final int PROBLEMS_TAB = 5;

    private JTabbedPane tabbedPanel;
    private Folder folder;
    private HomeTab homeTab;
    private FilesTab filesTab;
    private MembersTab membersTab;
    private FolderChatPanel folderChatPanel;
    private SettingsTab settingsTab;
//    private ProblemsTab problemsTab;
    //private MyFolderListener myFolderListener ;
    public FolderPanel(Controller controller) {
        super(controller);
      //  myFolderListener = new MyFolderListener();
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
        //Folder oldFolder = this.folder;
        this.folder = folder;
        membersTab.setFolder(folder);
        folderChatPanel.setFolder(folder);
        homeTab.setFolder(folder);
        settingsTab.setFolder(folder);
        tabbedPanel.setIconAt(HOME_TAB, Icons.getIconFor(folder.getInfo()));
        //problemsTab.setFolder(folder);
       // updateProblemsTab();
       // if (oldFolder != null) {
      //      oldFolder.removeFolderListener(myFolderListener);
      //  }
       // folder.addFolderListener(myFolderListener);
    }

   // private void updateProblemsTab() {
   //     JComponent problemTabUI = problemsTab.getUIComponent();
   //     if (folder.hasProblems()) {
   //         if (!(tabbedPanel.indexOfComponent(problemTabUI) == PROBLEMS_TAB )) {
   //             tabbedPanel.add(" " + problemsTab.getTitle() +" " ,problemTabUI );
   //         }
   //         problemsTab.update();
   //     } else {
   //         if (tabbedPanel.indexOfComponent(problemTabUI) == PROBLEMS_TAB ) {
   //             tabbedPanel.remove(problemTabUI);
   //         }
   //     }        
    //}
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

    /** */
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
     */
    public String getTitle() {
        if (folder != null) {
            return Translation.getTranslation("title.my.folders") + " > "
                + folder.getName() + " > " + getCurrentTab().getTitle();
        }
        return "";
    }

    /** i18n keys */
    private void initComponents() {
        tabbedPanel = new JTabbedPane();
        filesTab = new FilesTab(getController());
        membersTab = new MembersTab(getController());
        folderChatPanel = new FolderChatPanel(getController(),
            getUIController().getChatModel());
        homeTab = new HomeTab(getController());
        settingsTab = new SettingsTab(getController());
        //problemsTab = new ProblemsTab(getController());
        
        tabbedPanel.add(" "
            + homeTab.getTitle() + " ",
            homeTab.getUIComponent());
        tabbedPanel.setMnemonicAt(HOME_TAB, KeyEvent.VK_H);

        tabbedPanel.add(
            " " + filesTab.getTitle() + " ", filesTab
                .getUIComponent());
        tabbedPanel.setMnemonicAt(FILES_TAB, KeyEvent.VK_F);
        tabbedPanel.setIconAt(FILES_TAB, Icons.DIRECTORY);

        tabbedPanel.add(" "
            + membersTab.getTitle() + " ",
            membersTab.getUIComponent());
        tabbedPanel.setMnemonicAt(MEMBERS_TAB, KeyEvent.VK_M);
        tabbedPanel.setIconAt(MEMBERS_TAB, Icons.NODE_FRIEND_CONNECTED);

        tabbedPanel.add(
            " " + folderChatPanel.getTitle() + " ",
            folderChatPanel.getUIComponent());
        tabbedPanel.setMnemonicAt(CHAT_TAB, KeyEvent.VK_C);
        tabbedPanel.setIconAt(CHAT_TAB, Icons.CHAT);

        tabbedPanel.add(
            " " + settingsTab.getTitle()
                + " ", settingsTab.getUIComponent());
        tabbedPanel.setMnemonicAt(SETTINGS_TAB, KeyEvent.VK_S);
        // TODO add settings icon
        tabbedPanel.setIconAt(SETTINGS_TAB, null);

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
    
   // private class MyFolderListener implements FolderListener{

        //public boolean fireInEventDispathThread() {
       //     return true;
       // }

        //public void folderChanged(FolderEvent folderEvent) {
        //}

//        public void remoteContentsChanged(FolderEvent folderEvent) {
  //      }

    //    public void statisticsCalculated(FolderEvent folderEvent) {
      //  }

        //public void syncProfileChanged(FolderEvent folderEvent) {
//        }

  //      public void problemsFound(FolderEvent folderEvent) {
    //        Folder sourceFolder =(Folder)folderEvent.getSource(); 
      //      if (sourceFolder != folder) {
        //        return;
          //  }
            //updateProblemsTab();
        //}
        
    //}
    
}
