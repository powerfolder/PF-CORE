/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: CleanupTranslationFiles.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.jgoodies.binding.value.Trigger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.preferences.HTTPProxySettingsDialog;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

public class ConfigurationLoaderDialog extends PFUIComponent {

    private final Vector<String> serviceProviderUrls = new Vector<String>();

    private String initialText;
    private JFrame frame;
    private JComboBox<String> addressBox;
    private JComponent proxySettingsLabel;
    private JProgressBar progressBar;
    private JLabel infoLabel;
    private JButton okButton;
    private JCheckBox neverAskAgainBox;
    private Object haltLock = new Object();
    private Trigger finishedTrigger;

    public ConfigurationLoaderDialog(Controller controller, String initialText) {
        super(controller);
        this.initialText = initialText;
    }
    
    public ConfigurationLoaderDialog(Controller controller) {
        super(controller);
    }

    public ConfigurationLoaderDialog(Controller controller,
        Trigger finishedTrigger)
    {
        super(controller);
        this.finishedTrigger = finishedTrigger;
    }

    public void openAndWait() {
        try {
            UIUtil.invokeAndWaitInEDT(new Runnable() {
                public void run() {
                    getFrame().setAlwaysOnTop(true);
                    getFrame().setVisible(true);
                    getFrame().setAlwaysOnTop(false);
                }
            });
            if (!EventQueue.isDispatchThread()) {
                synchronized (haltLock) {
                    // HALT Main program until action.
                    haltLock.wait();
                }
            }
        } catch (Exception e) {
            logSevere(e);
        }
    }

    // Internal methods *******************************************************

    private JFrame getFrame() {
        if (frame == null) {
            initComponents();

            FormLayout layout = new FormLayout("max(p;150dlu), 3dlu, p",
                "p, 7dlu, p, 3dlu, p, 7dlu, p, 7dlu, 12dlu, 14dlu, p");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            CellConstraints cc = new CellConstraints();
            int row = 1;
            builder.addLabel(
                Translation.get("config.loader.dialog.info"),
                cc.xyw(1, row, 3));
            row += 2;
            builder.add(addressBox, cc.xy(1, row));
            builder.add(Help.createWikiLinkButton(getController(),
                WikiLinks.SERVER_CLIENT_DEPLOYMENT), cc.xy(3, row));

            row += 2;
            builder.add(proxySettingsLabel,
                cc.xywh(1, row, 1, 1, "right, center"));

            row += 2;
            builder.add(neverAskAgainBox, cc.xy(1, row));

            row += 2;
            builder.add(progressBar, cc.xyw(1, row, 3));
            builder.add(infoLabel, cc.xyw(1, row, 3));

            row += 2;
            Component buttonBar = buildButtonBar();
            builder.add(buttonBar, cc.xyw(1, row, 3));
            builder.getPanel().setBackground(Color.WHITE);

            frame = new JFrame(getTitle());
            //frame.setAlwaysOnTop(true);
            // frame.setUndecorated(true);
            frame.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
            frame.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));
            frame.setResizable(false);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    exit();
                }
            });
            frame.getContentPane().add(builder.getPanel());
            frame.getRootPane().setDefaultButton(okButton);
            frame.pack();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation((screen.width - frame.getWidth()) / 2,
                (screen.height - frame.getHeight() - 200) / 2);

            progressBar.setVisible(false);
            infoLabel.setVisible(false);
        }
        return frame;
    }

    private void addServerURL(String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        // Skip entries with google analytics stuff (e.g. in Basic distribution)
        if (!value.contains("utm_source")
            && !serviceProviderUrls.contains(value))
        {
            serviceProviderUrls.add(value);
        }
    }

    @SuppressWarnings("serial")
    private void initComponents() {
        if (StringUtils.isNotBlank(ConfigurationEntry.CONFIG_URL
            .getValue(getController())))
        {
            addServerURL(ConfigurationEntry.CONFIG_URL
                .getValue(getController()));
        }
        if (getController().getOSClient() != null
            && getController().getOSClient().hasWebURL())
        {
            addServerURL(getController().getOSClient().getWebURL());
        }
        if (ConfigurationEntry.PROVIDER_URL.hasValue(getController())) {
            addServerURL(ConfigurationEntry.PROVIDER_URL
                .getValue(getController()));
        }
        // addServerURL(ConfigurationEntry.PROVIDER_URL.getDefaultValue());

        addressBox = new JComboBox<>(serviceProviderUrls);
        addressBox.setEditable(true);
        
        try {
            JTextField editorField = (JTextField) addressBox.getEditor()
                .getEditorComponent();
            if (StringUtils.isNotBlank(initialText)) {
                editorField.setText(initialText);
            } else {                
                editorField.setText("http://");
            }
            editorField.setCaretPosition(editorField.getText().length());
        } catch (Exception e) {
            // Ignore
        }

        proxySettingsLabel = new ActionLabel(getController(),
            new AbstractAction(
                Translation.get("general.proxy_settings"))
            {
                public void actionPerformed(ActionEvent e) {
                    new HTTPProxySettingsDialog(getController(), frame).open();
                }
            }).getUIComponent();

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        infoLabel = SimpleComponentFactory.createLabel("X");

        neverAskAgainBox = SimpleComponentFactory.createCheckBox(Translation
            .get("general.neverAskAgain"));
        neverAskAgainBox.setVisible(false);
        try {
            boolean prompt = ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM
                .getValueBoolean(getController());
            boolean isPF = ServerClient.isPowerFolderCloud(getController());
            boolean branded = getController().getDistribution()
                .isBrandedClient();
            neverAskAgainBox.setVisible(prompt && isPF && !branded);
        } catch (Exception e) {
            logWarning(e.toString());
        }

    }

    private String getTitle() {
        return Translation.get("config.loader.dialog.title");
    }

    private Component buildButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showProgressBar();
                new LoadingWorking().start();
            }
        });

        JButton skipButton = new JButton(
            Translation.get("config.loader.dialog.skip"));
        skipButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
                frame.dispose();
                mainProgrammContinue();
            }
        });
        JButton exitButton = new JButton(
            Translation.get("config.loader.dialog.exit"));
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        JComponent bar;
        if (getController().isStarted()) {
            skipButton.setText(Translation.get("general.cancel"));
            bar = ButtonBarFactory.buildCenteredBar(okButton, skipButton);
        } else {
            bar = ButtonBarFactory.buildCenteredBar(okButton, exitButton);
        }

        bar.setOpaque(false);
        return bar;
    }

    private void showProgressBar() {
        infoLabel.setVisible(false);
        progressBar.setVisible(true);
    }

    private void showInfo(String text) {
        infoLabel.setText(text);
        progressBar.setVisible(false);
        infoLabel.setVisible(true);
    }

    /**
     * Creates an internationalized ok button
     * 
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    private static JButton createOKButton(ActionListener listener) {
        JButton okButton = new JButton(Translation.get("general.ok"));
        okButton.setMnemonic(Translation.get("general.ok.key")
            .trim().charAt(0));
        okButton.addActionListener(listener);
        return okButton;
    }

    // Application logic ******************************************************

    private void exit() {
        frame.setVisible(false);
        frame.dispose();
        mainProgrammContinue();
        getController().exit(0);
    }

    private void mainProgrammContinue() {
        synchronized (haltLock) {
            haltLock.notifyAll();
        }
    }

    private void saveConfig() {
        if (getController().isStarted()) {
            getController().saveConfig();
        } else {
            // Not started. Try a delayed store
            getController().schedule(() -> {
                getController().saveConfig();
            } , 5000L);
        }
    }

    private class LoadingWorking extends SwingWorker {
        @Override
        public Object construct() throws IOException {
            Properties preConfig = null;
            String input = (String) addressBox.getSelectedItem();
            try {
                preConfig = loadFromInput(input);
            } catch (IOException e) {
                logWarning("Unable to load config from " + input + ": " + e);
                if (StringUtils.isNotBlank(input) && input.indexOf(":", 7) < 0)
                {
                    try {
                        // Try harder with default port of web portal
                        preConfig = loadFromInput(input + ":8080");
                    } catch (IOException e2) {
                        // Try even harder to connect via TCP directly
                        if (!input.toLowerCase().startsWith("http")) {
                            Socket socket = null;
                            try {
                                InetSocketAddress addr = Util
                                    .parseConnectionString(input);
                                socket = new Socket();
                                socket.connect(addr,
                                    Constants.SOCKET_CONNECT_TIMEOUT);
                                if (socket.isConnected()) {
                                    logInfo("Got direct TCP connect to server "
                                        + input);
                                    ConfigurationEntry.SERVER_HOST.setValue(
                                        getController(), input);
                                    ConfigurationEntry.SERVER_NODEID
                                        .removeValue(getController());
                                    ConfigurationEntry.SERVER_WEB_URL
                                        .removeValue(getController());
                                    preConfig = new Properties();
                                    preConfig.put(
                                        ConfigurationEntry.SERVER_HOST
                                            .getConfigKey(), input);
                                    preConfig.put(
                                        ConfigurationEntry.SERVER_WEB_URL
                                            .getConfigKey(), "http://" + input
                                            + ":8080");
                                }
                            } catch (Exception e3) {
                                logInfo("Not direct TCP connect possible to "
                                    + input + ". " + e3);
                            } finally {
                                try {
                                    if (socket != null) {
                                        socket.close();
                                    }
                                } catch (Exception e3) {
                                }
                            }
                        }
                    }
                }
            }

            if (preConfig != null && !preConfig.isEmpty()
                && containsServerHost(preConfig))
            {
                ConfigurationLoader.merge(preConfig, getController()
                    .getConfig(), getController().getPreferences(), true);
                // Seems to be valid, store.
                saveConfig();
            }
            return preConfig;
        }

        private Properties loadFromInput(String input) throws IOException {
            Properties preConfig;
            ConfigurationEntry.CONFIG_URL.setValue(getController(), input);
            saveConfig();
            preConfig = ConfigurationLoader.loadPreConfiguration(input);
            if (preConfig != null && !containsServerWeb(preConfig)) {
                String finalURL = Util.removeLastSlashFromURI(input);
                if (!finalURL.startsWith("http")) {
                    finalURL = "http://" + finalURL;
                }
                logWarning("Server web URL not found in client config. Using fallback: "
                    + finalURL);
                preConfig.put(ConfigurationEntry.SERVER_WEB_URL.getConfigKey(),
                    finalURL);
            }
            return preConfig;
        }

        @Override
        public void finished() {
            Properties preConfig = (Properties) get();
            Throwable t = getThrowable();
            String errorMsg = null;
            if (preConfig == null) {
                errorMsg = Translation
                    .get("config.loader.dialog.error.generic");
                if (t != null) {
                    if (t instanceof FileNotFoundException) {
                        errorMsg = Translation
                            .get("config.loader.dialog.error.config.notfound");
                    } else if (t instanceof MalformedURLException) {
                        errorMsg = Translation
                            .get("config.loader.dialog.error.address.invalid");
                    } else if (t instanceof IllegalArgumentException) {
                        errorMsg = Translation
                            .get("config.loader.dialog.error.address.invalid");
                    } else if (t instanceof UnknownHostException) {
                        errorMsg = Translation
                            .get("config.loader.dialog.error.host.notfound");
                    } else {
                        errorMsg = t.getMessage();
                    }
                }
            } else if (preConfig.size() == 0) {
                errorMsg = Translation
                    .get("config.loader.dialog.error.config.empty");
            } else if (!containsServerHost(preConfig)) {
                errorMsg = Translation
                    .get("config.loader.dialog.error.server.missing");
            }
            if (errorMsg != null) {
                showInfo(Translation.get(
                    "config.loader.dialog.error", errorMsg));
                return;
            }

            // Success
            showInfo(Translation.get("config.loader.dialog.loaded",
                String.valueOf(preConfig.size())));
            frame.setVisible(false);
            frame.dispose();
            mainProgrammContinue();

            if (neverAskAgainBox.isSelected()
                && ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM
                    .getValueBoolean(getController()))
            {
                ConfigurationEntry.CONFIG_PROMPT_SERVER_IF_PF_COM.setValue(
                    getController(), false);
                saveConfig();
            }

            if (finishedTrigger != null) {
                finishedTrigger.triggerCommit();
            }
            if (getController().isStarted()) {
                handleRestartRequest();
            }
        }

        private boolean containsServerHost(Properties props) {
            return props.containsKey(ConfigurationEntry.SERVER_HOST
                .getConfigKey());
        }

        private boolean containsServerWeb(Properties props) {
            return props.containsKey(ConfigurationEntry.SERVER_WEB_URL
                .getConfigKey());
        }

        /**
         * Asks user about restart and executes that if requested
         */
        private void handleRestartRequest() {
            int result = DialogFactory.genericDialog(
                getController(),
                Translation.get("preferences.dialog.restart.title"),
                Translation.get("preferences.dialog.restart.text"),
                new String[]{
                    Translation
                        .get("preferences.dialog.restart.restart"),
                    Translation.get("general.cancel")}, 0,
                GenericDialogType.QUESTION); // Default is restart

            if (result == 0) { // Restart
                getController().shutdownAndRequestRestart();
            }
        }
    }
}
