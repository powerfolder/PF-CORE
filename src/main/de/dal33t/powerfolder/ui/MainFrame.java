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
package de.dal33t.powerfolder.ui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.Transferable;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.prefs.Preferences;
import java.io.IOException;
import java.io.File;

import javax.swing.*;
import javax.swing.plaf.RootPaneUI;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.action.SyncAllFoldersAction;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

/**
 * Powerfoldes gui mainframe
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.44 $
 */
public class MainFrame extends PFUIComponent {

    private JFrame uiComponent;

    private MainTabbedPane mainTabbedPane;

    /**
     * The menu bar that handles F5 for sync, etc.
     * This is not visible in the GUI.
     */
    private JMenuBar menuBar;

    /**
     * The status bar on the lower side of the screen.
     */
    private StatusBar statusBar;

    /**
     * @param controller
     *            the controller.
     * @throws HeadlessException
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

        FormLayout layout = new FormLayout("fill:pref:grow",
            "0dlu, pref, 1dlu, fill:0:grow, 1dlu, pref");
          // menu  head        body               footer
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 2dlu, 2dlu, 2dlu"));
        CellConstraints cc = new CellConstraints();

        builder.add(menuBar, cc.xy(1, 1));
        builder.add(new JLabel(Icons.LOGO400UI, SwingConstants.LEFT), cc.xy(1, 2));
        builder.add(mainTabbedPane.getUIComponent(), cc.xy(1, 4));
        builder.add(statusBar.getUIComponent(), cc.xy(1, 6));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.setBackground(Color.white);
        uiComponent.setResizable(true);

        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("mainframe4.x", 100), prefs.getInt(
            "mainframe4.y", 100));

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("mainframe4.width", 500);
        int height = prefs.getInt("mainframe4.height", 600);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        if (prefs.getBoolean("mainframe4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = uiComponent.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
            }
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent
            .setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // add window listener, checks if exit is needed on pressing X
		uiComponent.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
                if (OSUtil.isSystraySupported()) {
                    handleExitFirstRequest();
                    boolean quitOnX = PreferencesEntry.QUIT_ON_X
                            .getValueBoolean(getController());
                    if (quitOnX) {
                        exitProgram();
                    } else {
                        getUIController().hideChildPanels();
                        uiComponent.setVisible(false);
                    }
                } else {
                    // Quit if systray is not Supported by OS.
                    exitProgram();
                }
			}
            
            /**
             * Shuts down the program
             */
            private void exitProgram() {
                uiComponent.setVisible(false);
                uiComponent.dispose();
                new Thread("Close PowerFolder Thread") {
                    @Override
                    public void run() {
                        getController().tryToExit(0);
                    }
                }.start();
            }
        });

        uiComponent.setTransferHandler(new MyTransferHandler());

    }
    
    /**
     * Asks user about exit behaviour of the program
     * when the program is used for the first time
     */
    private void handleExitFirstRequest() {
		boolean askForQuitOnX = PreferencesEntry.ASK_FOR_QUIT_ON_X
				.getValueBoolean(getController());
		if (askForQuitOnX) {
			// Prompt for personal message.
			String[] options = { Translation.getTranslation("general.ok"),
					Translation.getTranslation("general.cancel") };
			FormLayout layout = new FormLayout("pref", "pref, pref, pref, pref");
			PanelBuilder builder = new PanelBuilder(layout);
			CellConstraints cc = new CellConstraints();
			JCheckBox checkbox = new JCheckBox(Translation
					.getTranslation("dialog.ask_for_quit_on_x.text"));
			checkbox.setSelected(!PreferencesEntry.QUIT_ON_X.getValueBoolean(getController()));
			builder.add(checkbox, cc.xy(1, 1));

			JPanel innerPanel = builder.getPanel();
			NeverAskAgainResponse response = DialogFactory.genericDialog(
                    getController(), Translation
							.getTranslation("dialog.ask_for_quit_on_x.title"),
					innerPanel, options, 0, GenericDialogType.QUESTION,
					Translation.getTranslation("general.neverAskAgain"));

			if (response.getButtonIndex() == 0) { // == OK
				boolean checked = checkbox.isSelected();
				if (checked) {
					// minimize to systray
					PreferencesEntry.QUIT_ON_X.setValue(getController(), false);
				}else{
					PreferencesEntry.QUIT_ON_X.setValue(getController(), true);
				}
			}
			if (response.isNeverAskAgain()) {
				// don't ask me again
				PreferencesEntry.ASK_FOR_QUIT_ON_X.setValue(getController(),
						false);
			}
		}
	}

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        logFine("Screen resolution: "
            + Toolkit.getDefaultToolkit().getScreenSize());

        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.getInstance().POWERFOLDER_IMAGE);

        createMenuBar();

        mainTabbedPane = new MainTabbedPane(getController());

        statusBar = new StatusBar(getController());

        updateTitle();
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        JMenuItem syncAllMenuItem = new JMenuItem(new SyncAllFoldersAction(getController()));
        menuBar.add(syncAllMenuItem);
    }

    /**
     * Updates the title
     */
    public void updateTitle() {

        StringBuilder title = new StringBuilder();
        
        String appName = Translation.getTranslation("general.application.name");
        // Urg
        if (StringUtils.isEmpty(appName) || appName.startsWith("- ")) {
            appName = "PowerFolder";
        }
        String initial = appName + " v" + Controller.PROGRAM_VERSION;
        if (getController().isVerbose()) {
            // Append in front of programm name in verbose mode
            title.append(getController().getMySelf().getNick() + " | "
                + initial);
        } else {
            // Otherwise append nick at end
            title.append(initial + " | "
                + getController().getMySelf().getNick());
        }

        if (getController().isVerbose()
            && getController().getBuildTime() != null)
        {
            title.append(" | build: " + getController().getBuildTime());
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.DAY_OF_MONTH) == 21
            && cal.get(Calendar.MONTH) == 2)
        {
            title.append(" | Happy birthday archi !");
        }
        uiComponent.setTitle(title.toString());
    }

    /**
     * @return the ui panel of the mainframe.
     */
    public JFrame getUIComponent() {
        return uiComponent;
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {
        // Store main window preferences
        Preferences prefs = getController().getPreferences();

        if ((uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
            prefs.putBoolean("mainframe4.maximized", true);
        } else {
            prefs.putInt("mainframe4.x", uiComponent.getX());
            if (uiComponent.getWidth() > 0) {
                prefs.putInt("mainframe4.width", uiComponent.getWidth());
            }
            prefs.putInt("mainframe4.y", uiComponent.getY());
            if (uiComponent.getHeight() > 0) {
                prefs.putInt("mainframe4.height", uiComponent.getHeight());
            }
            prefs.putBoolean("mainframe4.maximized", false);
        }
    }

    /**
     * @return true, if application is currently minimized
     */
    public boolean isIconified() {
        return (uiComponent.getExtendedState() & Frame.ICONIFIED) != 0;
    }

    /**
     * Determine if application is currently minimized or hidden (for example,
     * in the systray)
     * 
     * @return true, if application is currently minimized or hidden
     */
    public boolean isIconifiedOrHidden() {
        return isIconified() || !uiComponent.isVisible();
    }

    /**
     * Restore application from its minimized state
     */
    public void deiconify() {
        // Popup whole application
        uiComponent.setVisible(true);
        int state = uiComponent.getExtendedState();
        // Clear the iconified bit
        state &= ~Frame.ICONIFIED;
        // Deiconify the frame
        uiComponent.setExtendedState(state);
    }

    /**
     * Handler to accept folder drops, opening folder wizard.
     */
    private class MyTransferHandler extends TransferHandler {

        /**
         * Whether this drop can be imported; must be file list flavor.
         *
         * @param support
         * @return
         */
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        /**
         * Import the file. Only import if it is a single directory.
         *
         * @param support
         * @return
         */
        public boolean importData(TransferSupport support) {

            if (!support.isDrop()) {
                return false;
            }

            File file = getFileList(support);
            // @todo harry to import data 
            return file != null;
        }

        /**
         * Get the directory to import.
         * The transfer is a list of files; need to check the list has one
         * directory, else return null.
         *
         * @param support
         * @return
         */
        private File getFileList(TransferSupport support) {
            Transferable t = support.getTransferable();
            try {
                List list = (List) t.getTransferData(
                        DataFlavor.javaFileListFlavor);
                if (list.size() == 1) {
                    for (Object o : list) {
                        if (o instanceof File) {
                            File file = (File) o;
                            if (file.isDirectory()) {
                                return file;
                            }
                        }
                    }
                }
            } catch (UnsupportedFlavorException e) {
                logSevere(e);
            } catch (IOException e) {
                logSevere(e);
            }
            return null;
        }
    }
}