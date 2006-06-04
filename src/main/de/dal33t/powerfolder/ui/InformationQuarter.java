/* $Id: InformationQuarter.java,v 1.114.2.1 2006/04/29 10:00:52 schaatser Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.CardLayout;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.ui.chat.MemberChatPanel;
import de.dal33t.powerfolder.ui.folder.FolderPanel;
import de.dal33t.powerfolder.ui.friends.FriendsPanel;
import de.dal33t.powerfolder.ui.friends.FriendsSearchPanel;
import de.dal33t.powerfolder.ui.home.RootPanel;
import de.dal33t.powerfolder.ui.myfolders.MyFoldersPanel;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.ui.navigation.NavigationToolBar;
import de.dal33t.powerfolder.ui.navigation.PublicFoldersTreeNode;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.ui.recyclebin.RecycleBinPanel;
import de.dal33t.powerfolder.ui.transfer.DownloadsPanel;
import de.dal33t.powerfolder.ui.transfer.UploadsPanel;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;

/**
 * The information quarter right upper side of screen
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.114.2.1 $
 */
public class InformationQuarter extends PFUIComponent {
    // the ui panel
    private JComponent uiPanel;

    // The frame around the panel
    private SimpleInternalFrame uiFrame;

    // The control quarter to act on
    private ControlQuarter controlQuarter;

    private CardLayout cardLayout;
    private JPanel cardPanel = new JPanel();
    private static final String ROOT_PANEL = "root";
    private static final String FOLDER_PANEL = "folder";
    private static final String MYFOLDERS_PANEL = "myfolders";
    private static final String PUBLICFOLDERS_PANEL = "publicfolders";
    private static final String ONEPUBLICFOLDER_PANEL = "onepublicfolder";
    private static final String DOWNLOADS_PANEL = "downloads";
    private static final String UPLOADS_PANEL = "uploads";
    private static final String CHAT_PANEL = "chat";
    private static final String FRIENDS_PANEL = "friends";
    private static final String FRIENDSSEARCH_PANEL = "friendssearch";
    private static final String NETWORKSTATSISTICS_PANEL = "netstats";
    private static final String TEXT_PANEL = "text";
    private static final String RECYCLE_BIN_PANEL = "recycle";
    private static final String DEBUG_PANEL = "debug";
    
    // Root Panel
    private RootPanel rootPanel;

    // Folder panel
    private FolderPanel folderPanel;

    // MyFolders panel
    private MyFoldersPanel myFoldersPanel;

    // PublicFolders panel
    private PublicFoldersPanel publicFoldersPanel;

    // OnePublicFolder panel
    private OnePublicFolderPanel onePublicFolderPanel;

    // Down/uploads panel
    private DownloadsPanel downloadsPanel;
    private UploadsPanel uploadsPanel;

    private RecycleBinPanel recycleBinPanel;
    // chat
    private MemberChatPanel memberChatPanel;

    // friends
    private FriendsPanel friendsPanel;
    private FriendsSearchPanel friendsSearchPanel;

    //netstats
    private NetworkStatisticsPanel networkStatisticsPanel;
    
    // Text
    private TextPanel textPanel;
    
    // debug
    private DebugPanel debugPanel;

    /* The currently displayed item */
    private Object displayTarget;

    /**
     * 
     */
    public InformationQuarter(ControlQuarter controlQuarter,
        Controller controller)
    {
        super(controller);
        this.controlQuarter = controlQuarter;

        // Add selection behavior
        controlQuarter.getSelectionModel().addSelectionChangeListener(
            new ControlQuarterSelectionListener());
    }

    // Selection code *********************************************************

    /**
     * Main class to act on selection changes in the control quarter
     * 
     * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
     * @version $Revision: 1.114.2.1 $
     */
    private class ControlQuarterSelectionListener implements
        SelectionChangeListener
    {
        public void selectionChanged(SelectionChangeEvent selectionChangeEvent)
        {
            Object selection = selectionChangeEvent.getSelection();

            if (selection != null) {
                // Call our selection method
                setSelected(selection, controlQuarter.getSelectionParent());
            }
        }
    }

    private boolean showDebugReports() {
        if (!getController().isVerbose()) {
            return false;
        }
        Preferences pref = getController().getPreferences();
        return pref.getBoolean(DebugPanel.showDebugReportsPrefKey, false);   
    }
    
    /**
     * Sets the selected display component for info quarter
     * <p>
     */
    private void setSelected(Object selection, Object parentOfSelection) {
        log().verbose(
            "Selected " + selection + ", parent: " + parentOfSelection);

        if (selection instanceof Directory) {
            displayDirectory((Directory) selection);
        } else if (selection instanceof Folder) {
            displayFolder((Folder) selection);
        } else if (selection instanceof Member) {
            // chat only if selection on Friends or Connected treenode and
            // not running in verbose mode (=displays debug info about node)
            Member member = (Member) selection;
            if (!showDebugReports()) {
                displayChat(member);
            } else {
                displayNodeInformation((Member) selection);
            }

        } else if (selection instanceof RootNode) {
            displayRootPanel();
        } else if (selection instanceof FolderDetails) {
            displayOnePublicFolder((FolderDetails) selection);
        } else if (selection.equals("JOINED_FOLDERS")) {
            displayMyFolders();
        } else if (selection instanceof PublicFoldersTreeNode) {
            displayPublicFolders();
        } else if (selection == RootNode.DOWNLOADS_NODE_LABEL) {
            displayDownloads();
        } else if (selection == RootNode.UPLOADS_NODE_LABEL) {
            displayUploads();
        } else if (selection == RootNode.RECYCLEBIN_NODE_LABEL) {
            displayRecycleBinPanel();
        } else if (selection == RootNode.DEBUG_NODE_LABEL) {
            displayDebugPanel();
        } else if (selection == getController().getNodeManager()
            .getFriendsTreeNode())
        {
            displayFriendsPanel();
        } else if (selection == getController().getNodeManager()
            .getChatTreeNodes())
        {
            displayFriendsSearchPanel();
        } else if (selection == getController().getNodeManager()
            .getOnlineTreeNode())
        {
            displayStats();            
        } else {

            displayNothing();
        }
    }

    // UI Building ************************************************************

    /**
     * Returns the ui componennt and builds it lazily
     * 
     * @return
     */
    public JComponent getUIComponent() {
        if (uiPanel == null) {
            initComponents();

            FormLayout layout = new FormLayout("max(0;pref):grow, pref",
                "pref, 0:grow, pref, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // Content
            builder.add(cardPanel, cc.xywh(1, 2, 2, 2));

            uiFrame = new SimpleInternalFrame(Translation
                .getTranslation("infoside.title"));
            uiFrame.add(builder.getPanel());
            uiFrame.setToolBar(new NavigationToolBar(getController(),
                getUIController().getControlQuarter().getNavigationModel())
                .getUIComponent());
            uiPanel = uiFrame;
        }

        return uiPanel;
    }

    /**
     * Initializes all ui components
     */
    private void initComponents() {
        textPanel = new TextPanel();

        // Root panel
        rootPanel = new RootPanel(getController());

        // Folder panel
        folderPanel = new FolderPanel(getController());

        // MyFolders panel
        myFoldersPanel = new MyFoldersPanel(getController());

        // PublicFolders panel
        publicFoldersPanel = new PublicFoldersPanel(getController());

        // OnePublicFolder panel
        onePublicFolderPanel = new OnePublicFolderPanel(getController());
        
        recycleBinPanel = new RecycleBinPanel(getController());
        debugPanel = new DebugPanel(getController());
        // chat
        memberChatPanel = new MemberChatPanel(getController());

        // friends
        friendsPanel = new FriendsPanel(getController());
        friendsSearchPanel = new FriendsSearchPanel(getController());

        // Down/uploads panel
        downloadsPanel = new DownloadsPanel(getController());
        uploadsPanel = new UploadsPanel(getController());

        networkStatisticsPanel = new NetworkStatisticsPanel(getController());

        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);
        cardPanel.add(ROOT_PANEL, rootPanel.getUIComponent());
        cardPanel.add(FOLDER_PANEL, folderPanel.getUIComponent());
        cardPanel.add(MYFOLDERS_PANEL, myFoldersPanel.getUIComponent());
        cardPanel.add(PUBLICFOLDERS_PANEL, publicFoldersPanel.getUIComponent());
        cardPanel.add(ONEPUBLICFOLDER_PANEL, onePublicFolderPanel
            .getUIComponent());
        cardPanel.add(DOWNLOADS_PANEL, downloadsPanel.getUIComponent());
        cardPanel.add(UPLOADS_PANEL, uploadsPanel.getUIComponent());
        cardPanel.add(CHAT_PANEL, memberChatPanel.getUIComponent());
        cardPanel.add(FRIENDS_PANEL, friendsPanel.getUIComponent());
        cardPanel.add(FRIENDSSEARCH_PANEL, friendsSearchPanel.getUIComponent());
        cardPanel.add(NETWORKSTATSISTICS_PANEL, networkStatisticsPanel
            .getUIComponent());
        cardPanel.add(TEXT_PANEL, textPanel.getUIComponent());
        cardPanel.add(RECYCLE_BIN_PANEL, recycleBinPanel.getUIComponent());
        cardPanel.add(DEBUG_PANEL, debugPanel.getUIComponent());
    }

    /**
     * Answers the currently displayed target
     * 
     * @return
     */
    public Object getDisplayTarget() {
        return displayTarget;
    }

    /**
     * Sets the current display target
     * 
     * @param target
     */
    private void setDisplayTarget(Object target) {
        Object oldValue = displayTarget;
        displayTarget = target;

        // Fire property change
        firePropertyChange("displayTarget", oldValue, displayTarget);
    }

    /**
     * Answers if the item is currently displayed
     * 
     * @param target
     * @return
     */
    public boolean isDisplayed(Object target) {
        return Util.equals(displayTarget, target);
    }

    /**
     * Sets the title of the info frame. calling this method with
     * <code>null</code> will reset the title to default
     * 
     * @param title
     */
    public void setTitle(String title) {
        if (uiFrame != null) {
            if (title != null) {
                uiFrame.setTitle(title);
            } else {
                uiFrame.setTitle(Translation.getTranslation("infoside.title"));
            }
        }
    }

    // Display some really small statistics
    // FIXME internationalize
    private void displayStats() {
        setDisplayTarget(networkStatisticsPanel);
        cardLayout.show(cardPanel, NETWORKSTATSISTICS_PANEL);
        setTitle(networkStatisticsPanel.getTitle());  
        
        // Request network folders for statistics
        getController().getFolderRepository().requestNetworkFolderListIfRequired();
    }

    public void displayDebugPanel() {
        setDisplayTarget(debugPanel);
        cardLayout.show(cardPanel, DEBUG_PANEL);
        setTitle(debugPanel.getTitle());
    }    
    
    public void displayRecycleBinPanel() {
        setDisplayTarget(recycleBinPanel);
        cardLayout.show(cardPanel, RECYCLE_BIN_PANEL);
        setTitle(recycleBinPanel.getTitle());
    }
    
    public void displayRootPanel() {
        setDisplayTarget(rootPanel);
        cardLayout.show(cardPanel, ROOT_PANEL);
        setTitle(rootPanel.getTitle());
    }

    public void displayOnePublicFolder(FolderDetails folderDetails) {
        onePublicFolderPanel.setFolderInfo(folderDetails);
        setDisplayTarget(folderDetails);
        cardLayout.show(cardPanel, ONEPUBLICFOLDER_PANEL);
        setTitle(onePublicFolderPanel.getTitle());
    }

    public void displayFolder(Folder folder) {
        setDisplayTarget(folder);
        if (folderPanel != null) { // fixes rare NPE on start
            folderPanel.setFolder(folder);
            cardLayout.show(cardPanel, FOLDER_PANEL);
            setTitle(folderPanel.getTitle());
        }
    }

    /**
     * Displays a Directory from a Folder
     * 
     * @param directory
     *            The Directory to display
     */
    public void displayDirectory(Directory directory) {
        controlQuarter.setSelected(directory);
        setDisplayTarget(directory);
        folderPanel.setDirectory(directory);
        cardLayout.show(cardPanel, FOLDER_PANEL);
        setTitle(folderPanel.getTitle());
    }

    /**
     * Displays the downloads
     */
    public void displayDownloads() {
        setDisplayTarget(downloadsPanel);
        cardLayout.show(cardPanel, DOWNLOADS_PANEL);
        setTitle(downloadsPanel.getTitle());
    }

    /**
     * Displays the uploads
     */
    public void displayUploads() {
        setDisplayTarget(uploadsPanel);
        cardLayout.show(cardPanel, UPLOADS_PANEL);
        setTitle(uploadsPanel.getTitle());
    }

    private void displayFriendsPanel() {
        setDisplayTarget(friendsPanel);
        cardLayout.show(cardPanel, FRIENDS_PANEL);
        setTitle(friendsPanel.getTitle());
    }
    
    public void displayFriendsSearchPanel() {
        setDisplayTarget(friendsSearchPanel);
        cardLayout.show(cardPanel, FRIENDSSEARCH_PANEL);
        setTitle(friendsSearchPanel.getTitle());
    }

    /**
     * Displays the chat about a folder
     */
    public void displayChat(Folder folder) {
        displayFolder(folder);
        folderPanel.setTab(FolderPanel.CHAT_TAB);
    }

    /**
     * Displays the chat about a (friend) member
     */
    public void displayChat(Member member) {
        memberChatPanel.chatWithMember(member);
        setDisplayTarget(memberChatPanel);
        cardLayout.show(cardPanel, CHAT_PANEL);
        setTitle(memberChatPanel.getTitle());
    }

    /**
     * Displays myFolders
     */
    public void displayMyFolders() {
        setDisplayTarget(myFoldersPanel);
        cardLayout.show(cardPanel, MYFOLDERS_PANEL);
        setTitle(myFoldersPanel.getTitle());
    }

    /**
     * Displays publicFolders
     */
    public void displayPublicFolders() {
        setDisplayTarget(publicFoldersPanel);
        cardLayout.show(cardPanel, PUBLICFOLDERS_PANEL);
        setTitle(publicFoldersPanel.getTitle());

        // Request network folder list
        getController().getFolderRepository()
            .requestNetworkFolderListIfRequired();
    }

    /**
     * Displays the nodeinformation
     * 
     * @param ni
     */
    public void displayNodeInformation(Member node) {
        String debugReport = Debug.loadDebugReport(node.getInfo());
        if (debugReport != null) {
            setDisplayTarget(debugReport);
            displayText(debugReport);
        } else {
			String text = node.getNick() + " last seen online on "
					+ Format.formatDate(node.getLastNetworkConnectTime());
			text += "\nfrom " + node.getHostName();
			setDisplayTarget(node);
			displayText(text);
		}
        setTitle(null);
    }
    
    /**
     * Displays nothing
     */
    public void displayNothing() {
        setDisplayTarget(null);
        displayText("");
        setTitle(null);
    }

    /**
     * Switches to text display and shows the document
     * 
     * @param doc
     * @param autoScroll
     *            if text area shoud scroll to the end automatically
     */
    private void displayText(StyledDocument doc, boolean autoScroll) {
        setDisplayTarget(textPanel);
        textPanel.setText(doc, autoScroll);
        cardLayout.show(cardPanel, TEXT_PANEL);
    }

    /**
     * Displays the text
     * 
     * @param text
     */
    public void displayText(String text) {
        StyledDocument doc = new DefaultStyledDocument();
        try {
            doc.insertString(0, text, null);
        } catch (BadLocationException e) {
            log().verbose(e);
        }
        displayText(doc, false);
    }

    public FolderPanel getFolderPanel() {
        return folderPanel;
    }

}