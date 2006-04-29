/* $Id: MainFrame.java,v 1.44 2006/03/07 09:27:20 schaatser Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

import javax.swing.*;

import sun.font.FontManager;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.ui.navigation.ControlQuarter;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;

/**
 * Powerfoldes gui mainframe
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {
    private JFrame uiComponent;

    /** The toolbar ontop */
    private JComponent toolbar;

    /** Online state info field */
    private JLabel onlineStateInfo;

    /** The main split pane */
    private JSplitPane mainPane;

    /* left side */
    private ControlQuarter controlQuarter;

    /* right side */
    private InformationQuarter informationQuarter;

    /** Indicates that the user has a low screen resolution */
    private boolean lowScreenResolution;

    /**
     * @throws java.awt.HeadlessException
     */
    public MainFrame(Controller controller) throws HeadlessException {
        super(controller);
    }

    /**
     * Builds the UI
     */
    public void buildUI() {
        if (uiComponent == null) {
            // Initalize components
            initComponents();
        }

        //        
        // DropTarget dt = new DropTarget(uiComponent,
        // DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetListener() {
        //
        // public void dragEnter(DropTargetDragEvent dtde) {
        // //log().warn("dragEnter " + dtde.getCurrentDataFlavorsAsList());
        // }
        //
        // public void dragOver(DropTargetDragEvent dtde) {
        // log().warn("dragOver " + dtde);
        //                
        // }
        //
        // public void dropActionChanged(DropTargetDragEvent dtde) {
        // log().warn("dropActionChanged " + dtde);
        //                
        // }
        //
        // public void drop(DropTargetDropEvent dtde) {
        // Transferable t = dtde.getTransferable();
        // DataFlavor[] f = t.getTransferDataFlavors();
        // dtde.acceptDrop(dtde.getDropAction());
        // if (f.length > 0) {
        // try {
        // Object content = t.getTransferData(f[0]);
        // log().warn("Drop received: " + content.getClass() + ", " + content);
        // if (content instanceof List) {
        // List list = (List) content;
        // for (Iterator it = list.iterator(); it
        // .hasNext();)
        // {
        // Object element = (Object) it.next();
        // log().warn("Content: " + element.getClass());
        // }
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // } catch (UnsupportedFlavorException e) {
        // e.printStackTrace();
        // }
        // }
        // // log().warn("drop " + dtde);
        //                
        // }
        //
        // public void dragExit(DropTargetEvent dte) {
        // log().warn("dragExit " + dte);
        //                
        // }});
        //        
        //        
        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 4dlu, fill:0:grow, 1dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(toolbar, cc.xy(1, 1));

        builder.add(mainPane, cc.xy(1, 3));
        builder.add(onlineStateInfo, cc.xy(1, 5));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("mainframe.x", 100), prefs.getInt(
            "mainframe.y", 100));

        mainPane.setContinuousLayout(true);
        mainPane.setResizeWeight(0.3);

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("mainframe.width", 950);
        int height = prefs.getInt("mainframe.height", 630);
        uiComponent.setSize(width, height);
        // uiComponent.setSize(950, 630);

        // Now set divider location
        int defaultDividerLocation = (int) ((mainPane.getWidth() - mainPane
            .getDividerSize()) / 3.4);
        mainPane.setDividerLocation(getController().getPreferences().getInt(
            "mainframe.dividerlocation", defaultDividerLocation));

        if (prefs.getBoolean("mainframe.maximized", false)) {
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // add window listener, checks if exit is needed on pressing X
        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                boolean quitOnX = getController().getPreferences().getBoolean(
                    "quitonx", false);
                if (quitOnX || !Util.isSystraySupported()) {
                    // Quit if quit onx is active or not running with system
                    // tray
                    getController().exit(0);
                } else {
                    uiComponent.setVisible(false);
                }
            }
        });

    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        log()
            .debug(
                "Screen resolution: "
                    + Toolkit.getDefaultToolkit().getScreenSize());
        lowScreenResolution = Toolkit.getDefaultToolkit().getScreenSize()
            .getWidth() < 800;

        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.POWERFOLDER_IMAGE);
        // TODO: Maybe own theme: uiComponent.setUndecorated(true);

        // First of all, build all elements
        controlQuarter = new ControlQuarter(getController());
        informationQuarter = new InformationQuarter(controlQuarter,
            getController());

        mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlQuarter
            .getUIComponent(), informationQuarter.getUIComponent());
        mainPane.setDividerSize(6);
        mainPane.setOneTouchExpandable(true);
       

        controlQuarter.setSelected(controlQuarter.getNavigationTreeModel()
            .getRootNode());
        // Remove borders if possible (also from divider)
        Util.removeSplitPaneBorder(mainPane);

        // Add behavior for l&f changes
        mainPane.addPropertyChangeListener("UI", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                // Remove splitpane on mainpane on l&f change
                Util.removeSplitPaneBorder(mainPane);
            }
        });

        // Create online state info
        onlineStateInfo = ComplexComponentFactory
            .createOnlineStateLabel(getController());
        // Add behavior
        onlineStateInfo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // open connect dialog
                getUIController().getConnectAction().actionPerformed(null);
            }
        });

        // Create toolbar
        toolbar = createToolbar();

        updateTitle();
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JComponent createToolbar() {
        // Build the toolbar
        JButton wizardButton = createToolbarButton(getUIController()
            .getOpenWizardAction(), Icons.WIZARD_OPEN);

        JButton createFolderButton = createToolbarButton(getUIController()
            .getFolderCreateAction(), Icons.NEW_FOLDER);

        JButton inviteToFolderButton = createToolbarButton(getUIController()
            .createToolbarInvitationAction());

        JButton syncFoldersButton = createSyncNowToolbarButton();

        JButton toggleSilentModeButton = createToolbarButton(getUIController()
            .getToggleSilentModeAction());

        JButton preferencesButton = createToolbarButton(getUIController()
            .getOpenPreferencesAction(), Icons.PREFERENCES);

        JButton aboutButton = createToolbarButton(getUIController()
            .getOpenAboutAction(), Icons.ABOUT);

        ButtonBarBuilder bar2 = ButtonBarBuilder.createLeftToRightBuilder();
        // bar2.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        bar2.addFixed(wizardButton);
        bar2.addRelatedGap();
        bar2.addFixed(createFolderButton);
        bar2.addRelatedGap();
        bar2.addFixed(inviteToFolderButton);
        bar2.addRelatedGap();
        bar2.addFixed(syncFoldersButton);
        bar2.addRelatedGap();
        bar2.addFixed(toggleSilentModeButton);
        bar2.addRelatedGap();
        bar2.addFixed(preferencesButton);
        bar2.addRelatedGap();
        bar2.addFixed(aboutButton);

        // Create toolbar
        // JComponent bar = ButtonBarFactory.buildLeftAlignedBar(new JButton[]{
        // wizardButton,
        // // connectButton,
        // createFolderButton, joinLeaverFolderButton, scanFoldersButton,
        // toggleSilentModeButton, preferencesButton, aboutButton});

        // SimpleInternalFrame toolbarFrame = new SimpleInternalFrame(null);
        // Border border = BorderFactory.createCompoundBorder(new
        // SimpleInternalFrame.ShadowBorder(), Borders.DLU4_BORDER);
        // bar.setBorder(border);
        // bar.setBackground(Color.WHITE);
        // bar.setBorder(Borders.DLU4_BORDER);
        // toolbarFrame.add(bar);

        return bar2.getPanel();
    }

    /**
     * Creates a button ready to be used on the toolbar
     * 
     * @param action
     * @param mNemonic
     * @return
     */
    private JButton createToolbarButton(Action action) {
        return createToolbarButton(action, null);
    }

    /**
     * Creates a button ready to be used on the toolbar. Overrides the default
     * icon if set and removes the text
     * 
     * @param action
     * @param mNemonic
     * @return
     */
    private JButton createToolbarButton(Action action, final Icon icon) {
        final JButton button = new JButton(action);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        if (icon != null) {
            // Override icon
            button.setIcon(icon);
        }

        String tooltip = button.getToolTipText();

        // #FDE592
        String fontname = FontManager.getDefaultPhysicalFont()
            .getPostscriptName();
        tooltip = "<HTML><BODY bgcolor=\"#FFF6DA\"><font size=4 FACE=\"" + fontname + "\">&nbsp;"
            + tooltip + "&nbsp;</font></BODY></HTML>";        
        button.setToolTipText(tooltip);

        // Toolbar icons do not have texts!
        button.addPropertyChangeListener(new PropertyChangeListener() {
            boolean changeFromHere = false;

            public void propertyChange(PropertyChangeEvent evt) {
                if (!changeFromHere
                    && JButton.ICON_CHANGED_PROPERTY.equals(evt
                        .getPropertyName()))
                {
                    // log().warn("Button change: " + evt);
                    // String newText = (String) evt.getNewValue();
                    // if (!StringUtils.isEmpty(newText)) {
                    // changeFromHere = true;
                    // //button.setText(null);
                    // button.setIcon(icon);
                    // changeFromHere = false;
                    // }

                }
            }
        });
        
        button.setText(null);
        
        // Display tooltip immediately instead of waiting the default 1-2 seconds that Swing defaults to
        ToolTipManager.sharedInstance().registerComponent(button);
        button.addMouseListener( new MouseAdapter() {
        	private final String SHOW_TOOLTIP = "postTip";
        	
			@Override
			public void mouseEntered(MouseEvent e) {
				JComponent c = (JComponent) e.getComponent();
		        Action action = c.getActionMap().get(SHOW_TOOLTIP);

		        if (action != null) {
		            action.actionPerformed(new ActionEvent(c, ActionEvent.ACTION_PERFORMED, SHOW_TOOLTIP));
		        }
			}        	       	
        });
        
        // button.setDisplayedMnemonicIndex()

        Dimension dims;
        if (!lowScreenResolution) {
            dims = new Dimension(Icons.NEW_FOLDER.getIconWidth() + 50,
                Icons.NEW_FOLDER.getIconHeight() + 15);
        } else {
            dims = new Dimension(Icons.NEW_FOLDER.getIconWidth() + 35,
                Icons.NEW_FOLDER.getIconHeight() + 10);
        }
        button.setPreferredSize(dims);

        return button;
    }

    /**
     * Create the syncnow button. State is adapted from folderepository (if
     * scanning)
     * 
     * @return
     */
    private JButton createSyncNowToolbarButton() {
        final JButton syncNowButton = createToolbarButton(getUIController()
            .getScanAllFoldersAction(), Icons.SYNC_NOW);

        // Adapt state from folder repository
        getController().getFolderRepository().addFolderRepositoryListener(
            new FolderRepositoryListener() {
                public void unjoinedFolderAdded(FolderRepositoryEvent e) {
                }

                public void folderRemoved(FolderRepositoryEvent e) {
                }

                public void folderCreated(FolderRepositoryEvent e) {
                }

                public void scansStarted(FolderRepositoryEvent e) {
                    syncNowButton.setIcon(Icons.SYNC_NOW_ACTIVE);
                }

                public void scansFinished(FolderRepositoryEvent e) {
                    syncNowButton.setIcon(Icons.SYNC_NOW);
                }

                public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
                }
            });

        return syncNowButton;
    }

    /**
     * Updates the title
     */
    public void updateTitle() {

        String title = "PowerFolder v" + Controller.PROGRAM_VERSION;
        if (getController().isVerbose()) {
            // Append in front of programm name in verbose mode
            title = getController().getMySelf().getNick() + " | " + title;
        } else {
            // Otherwise append nick at end
            title += " | " + getController().getMySelf().getNick();
        }

        if (getController().isVerbose()
            && getController().getBuildTime() != null)
        {
            title += " | build: " + getController().getBuildTime();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.DAY_OF_MONTH) == 21
            && cal.get(Calendar.MONTH) == 2)
        {
            title += " | Happy birthday archi !";
        }
        uiComponent.setTitle(title);
    }

    /**
     * Returns the ui component
     * 
     * @return
     */
    public JFrame getUIComponent() {
        return uiComponent;
    }

    // public void windowStartAnimation() {
    // Preferences prefs = getController().getPreferences();
    //
    // double x = prefs.getInt("mainframe.x", 100);
    // double y = prefs.getInt("mainframe.y", 100);
    // double height = prefs.getInt("mainframe.height", 600);
    // double width = prefs.getInt("mainframe.width", 900);
    //
    // long animationTime = 15;
    //
    // int preWidth = 100;
    // int preHeight = 100;
    // double xSpeed = (width - preWidth) / animationTime / 2;
    // double ySpeed = (height - preHeight) / animationTime / 2;
    //
    // double curX = x + width / 2 - preWidth;
    // double curY = y + height / 2 - preHeight;
    // double curW = preWidth;
    // double curH = preHeight;
    //
    // log().warn("xSpeed: " + xSpeed + ", ySpeed " + ySpeed);
    // for (int i = 0; i < animationTime; i++) {
    // uiComponent.setSize((int) curW, (int) curH);
    // uiComponent.setLocation((int) curX, (int) curY);
    //
    // curX = curX - xSpeed;
    // curW = curW + xSpeed * 2;
    // curY = curY - ySpeed;
    // curH = curH + ySpeed * 2;
    //
    //            
    // try {
    // Thread.sleep(10);
    // } catch (InterruptedException e) {
    // }
    // }
    // }

    /**
     * Stores all current window valus FIXME: This does sometimes to correctcly
     * shut down
     */
    public void storeValues() {
        // Store main window preferences
        Preferences prefs = getController().getPreferences();

        if ((getUIComponent().getExtendedState() & Frame.MAXIMIZED_BOTH) != Frame.MAXIMIZED_BOTH)
        {
            prefs.putInt("mainframe.x", getUIComponent().getX());
            prefs.putInt("mainframe.width", getUIComponent().getWidth());
            prefs.putInt("mainframe.y", getUIComponent().getY());
            prefs.putInt("mainframe.height", getUIComponent().getHeight());
            prefs.putBoolean("mainframe.maximized", false);
        } else {
            prefs.putBoolean("mainframe.maximized", true);
        }
        prefs
            .putInt("mainframe.dividerlocation", mainPane.getDividerLocation());
    }

    /*
     * Exposing ***************************************************************
     */

    ControlQuarter getControlQuarter() {
        return controlQuarter;
    }

    InformationQuarter getInformationQuarter() {
        return informationQuarter;
    }
}