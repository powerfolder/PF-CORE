/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.event.FolderRepositoryAdapter;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import jwf.WizardPanel;

/**
 * PFC-2638: Desktop-Sync
 * @author Sprajc
 */
@SuppressWarnings("serial")
public class DesktopSyncSetupPanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(DesktopSyncSetupPanel.class.getName());

    private static final String WALLPAPER_CHANGER_EXE = "WallpaperChanger.exe";
    private static final String WALLPAPERS_DIR = "wallpapers";

    private WizardPanel nextPanel;
    private JTextArea infoLabel;
    private ActionLabel agreeLabel;
    private ActionLabel skipLabel;
    private JCheckBox wallpaperBox;
    private boolean agreed;
    private UserDirectory desktopDir;
    private ServerClient client;
    private FolderRepositoryListener postProcessor;

    public DesktopSyncSetupPanel(Controller controller, WizardPanel nextPanel,
        ServerClient client)
    {
        super(controller);
        Reject.ifNull(nextPanel, "Nextpanel is null");
        Reject.ifNull(client, "Client");
        this.nextPanel = nextPanel;
        this.agreed = false;
        this.client = client;
    }

    /**
     * PFC-2638: Start: Desktop sync option: Helper to insert step into wizard.
     * 
     * @param controller
     * @param nextPanel
     * @return
     */
    public static WizardPanel insertStepIfAvailable(Controller controller,
        WizardPanel nextPanel, ServerClient client)
    {
        if (!offerOption(controller)) {
            return nextPanel;
        }
        Reject.ifNull(client, "Client");
        if (!client.isAllowedToCreateFolders()) {
            return nextPanel;
        }
        boolean desktopDirAvailable = UserDirectories.getDesktopDirectory() != null;
        if (!desktopDirAvailable) {
            return nextPanel;
        }
        nextPanel = new DesktopSyncSetupPanel(controller, nextPanel, client);
        return nextPanel;
    }

    public static boolean offerOption(Controller controller) {
        return isFirstTime(controller)
            && ConfigurationEntry.SHOW_DESKTOP_SYNC_OPTION
                .getValueBoolean(controller);
    }

    /**
     * @return true if this is the first time of the wizard on this device.
     */
    private static boolean isFirstTime(Controller controller) {
        return controller.getPreferences().getBoolean("openwizard_desktop",
            true);
    }

    private void setFirstTime() {
        getController().getPreferences()
            .putBoolean("openwizard_desktop", false);
    }

    @Override
    protected void afterDisplay() {
        super.afterDisplay();
        setFirstTime();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public WizardPanel next() {
        if (!agreed) {
            return nextPanel;
        }
        return new FolderCreatePanel(getController());
    }

    @Override
    protected String getTitle() {
        return Translation.get("wizard.desktop_sync.title");
    }

    @Override
    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("9dlu, 120dlu:grow",
            "pref, 9dlu, pref, 1dlu, pref, 10dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(infoLabel, cc.xyw(1, row, 2));
        row += 2;
        builder.add(agreeLabel.getUIComponent(), cc.xyw(1, row, 2));
        row += 2;
        builder.add(wallpaperBox, cc.xyw(2, row, 1));
        row += 2;
        builder.add(skipLabel.getUIComponent(), cc.xyw(2, row, 1));
        row += 2;

        return builder.getPanel();
    }

    @Override
    protected void initComponents() {
        infoLabel = new JTextArea(
            Translation.get("wizard.desktop_sync.info_text"));
        infoLabel.setEditable(false);
        infoLabel.setCursor(null);
        infoLabel.setOpaque(false);
        infoLabel.setFocusable(false);
        infoLabel.setFont(UIManager.getFont("Label.font"));
        infoLabel.setEditable(false);
        infoLabel.setOpaque(false);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setLineWrap(true);

        agreeLabel = new ActionLabel(getController(), new AgreeAction(
            getController()));
        agreeLabel.convertToBigLabel();

        Account account = client.getAccount();
        boolean freeUser = account == null || !account.isProUser();
        wallpaperBox = SimpleComponentFactory.createCheckBox(Translation
            .get("wizard.desktop_sync.wallpaper"));
        wallpaperBox.setSelected(freeUser && setWallpaperAvailable());
        wallpaperBox.setVisible(setWallpaperAvailable());

        skipLabel = new ActionLabel(getController(), new SkipAction(
            getController()));
        skipLabel.convertToBigLabel();
        skipLabel.setIcon(null);

    }

    private boolean setWallpaperAvailable() {
        return (OSUtil.isWindowsSystem() || OSUtil.isMacOS())
            && ConfigurationEntry.SHOW_WALLPAPER_OPTION
                .getValueBoolean(getController());
    }

    private synchronized void setWallpaper() {
        Path wpTempDir = Controller.getTempFilesLocation().resolve(
            WALLPAPERS_DIR);
        try {
            wpTempDir = Files.createDirectories(wpTempDir);
        } catch (IOException e) {
            LOG.warning("Unable to create temporary directory for wallpapers at "
                + wpTempDir + ". " + e);
            return;
        }

        // Copy WallPaperChanger.exe
        Path wallpaperChangerEXE = wpTempDir.resolve(WALLPAPER_CHANGER_EXE);
        wallpaperChangerEXE.toFile().deleteOnExit();
        Util.copyResourceTo(WALLPAPER_CHANGER_EXE, WALLPAPERS_DIR,
            wallpaperChangerEXE, true, true);
        if (wallpaperChangerEXE == null || Files.notExists(wallpaperChangerEXE))
        {
            LOG.warning("Unable to install helper at " + wallpaperChangerEXE);
            return;
        }
        // Copy end

        // Copy wallpaper pics
        Util.copyResourceTo("7.png", WALLPAPERS_DIR,
            wpTempDir.resolve("7.png"), true, true);
        Util.copyResourceTo("9.png", WALLPAPERS_DIR,
            wpTempDir.resolve("9.png"), true, true);
        Path wallpaper9Path = wpTempDir.resolve("9.png");
        // Copy end

        LOG.fine("Setting Desktop wallpaper to " + wpTempDir.toAbsolutePath());
        String[] cmd;
        if (OSUtil.isWindowsSystem()) {
            String command = "\""
                + wallpaperChangerEXE.toAbsolutePath().toString() + "\"";
            command += " \"";
            command += wpTempDir.toAbsolutePath();
            command += "\"";
            command += " 2"; // Streched
            command += " \"";
            command += wpTempDir.toAbsolutePath();
            command += "\"";
            cmd = new String[]{command};
        } else if (OSUtil.isMacOS() && Files.exists(wallpaper9Path)) {
            // osascript -e 'tell application "Finder" to set desktop picture to
            // POSIX file "/Library/Desktop Pictures/Earth Horizon.jpg"'
            String command = "tell application \"Finder\" to set desktop picture to POSIX file";
            command += " \"";
            command += wallpaper9Path.toAbsolutePath().toString();
            command += "\"";
            command += "";
            cmd = new String[]{"osascript", "-e", command};
        } else {
            LOG.warning("Unable to set wallpaper. dir: " + wpTempDir
                + " file: " + wallpaper9Path);
            return;
        }

        LOG.fine("Executing command " + Arrays.asList(cmd));
        try {
            final Process p = Runtime.getRuntime().exec(cmd);
            // Auto-kill after 20 seconds
            getController().schedule(() -> {
                p.destroy();
            }, 20000L);
            byte[] out = StreamUtils.readIntoByteArray(p.getInputStream());
            @SuppressWarnings("unused")
            String output = new String(out);
            byte[] err = StreamUtils.readIntoByteArray(p.getErrorStream());
            String error = new String(err);

            // Wait for process to stop
            int res = p.waitFor();
            // PFS-844: Get output after process has ended.
            out = StreamUtils.readIntoByteArray(p.getInputStream());
            output += new String(out);
            err = StreamUtils.readIntoByteArray(p.getErrorStream());
            error += new String(err);
            if (res == 0) {
                LOG.info("Desktop wallpaper successfully set");
            } else {
                LOG.warning("Failed to set Desktop wallpaper: " + error);
            }
        } catch (Exception e) {
            LOG.info("Failed to set Desktop wallpaper " + e);
        }
    }

    private class AgreeAction extends BaseAction {

        protected AgreeAction(Controller controller) {
            super("action_desktop_sync.agree", controller);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            agreed = true;

            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, nextPanel);
            getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

            desktopDir = UserDirectories.getDesktopDirectory();
            FolderCreateItem item = new FolderCreateItem(
                desktopDir.getDirectory());
            getWizardContext().setAttribute(FOLDER_CREATE_ITEMS,
                Collections.singletonList(item));

            boolean exists = getController().getFolderRepository()
                .findExistingFolder(desktopDir.getDirectory()) != null;
            if (!exists) {
                postProcessor = new FolderCreatePostProcessor();
                getController().getFolderRepository()
                    .addFolderRepositoryListener(postProcessor);
            }

            if (wallpaperBox.isSelected()) {
                Runnable setter = new Runnable() {
                    @Override
                    public void run() {
                        setWallpaper();
                    }
                };
                getController().getIOProvider().startIO(setter);
            }

            getWizard().next();
        }
    }

    private class SkipAction extends BaseAction {
        protected SkipAction(Controller controller) {
            super("action_desktop_sync.skip", controller);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            getWizard().next();
        }
    }

    private class FolderCreatePostProcessor extends FolderRepositoryAdapter
    {
        @Override
        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public void folderCreated(FolderRepositoryEvent e) {
            if (!e.getFolder().getCommitOrLocalDir()
                .equals(desktopDir.getDirectory()))
            {
                return;
            }

            // Don't sync link files.
            e.getFolder().addPattern("*.lnk");

            getController().getFolderRepository()
                .removeFolderRepositoryListener(postProcessor);
        }

        @Override
        public void folderMoved(FolderRepositoryEvent e) {
            folderCreated(e);
        }
    }
}
