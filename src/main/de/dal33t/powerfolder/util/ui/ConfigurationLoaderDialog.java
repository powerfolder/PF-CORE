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
package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.preferences.HTTPProxySettingsDialog;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;

public class ConfigurationLoaderDialog extends PFUIComponent {

    private static final String[] SERVICE_PROVIDER_URLS = {"http://www.powerfolder.com",
    // , "http://relay001.node.powerfolder.com:7777"
    };

    private JFrame frame;
    private JComboBox addressBox;
    private JComponent proxySettingsLabel;
    private JProgressBar progressBar;
    private JLabel infoLabel;
    private JButton okButton;
    private Object haltLock = new Object();
    private Trigger finishedTrigger;

    public ConfigurationLoaderDialog(Controller controller) {
        super(controller);
    }
    
    public ConfigurationLoaderDialog(Controller controller, Trigger finishedTrigger) {
        super(controller);
        this.finishedTrigger = finishedTrigger;
    }

    public void openAndWait() {
        try {
            UIUtil.invokeAndWaitInEDT(new Runnable() {
                public void run() {
                    getFrame().setVisible(true);
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

            FormLayout layout = new FormLayout("p:g, 3dlu, p",
                "p, 7dlu, p, 3dlu, p, 7dlu, 12dlu, 14dlu, p");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            CellConstraints cc = new CellConstraints();
            int row = 1;
            builder
                .addLabel(Translation
                    .getTranslation("config.loader.dialog.info"), cc.xyw(1,
                    row, 3));
            row += 2;
            builder.add(addressBox, cc.xy(1, row));
            builder.add(Help.createWikiLinkButton(getController(),
                WikiLinks.SERVER_CLIENT_DEPLOYMENT), cc.xy(3, row));

            row += 2;
            builder.add(proxySettingsLabel, cc.xywh(1, row, 1, 1,
                "right, center"));

            row += 2;
            builder.add(progressBar, cc.xyw(1, row, 3));
            builder.add(infoLabel, cc.xyw(1, row, 3));

            row += 2;
            Component buttonBar = buildButtonBar();
            builder.add(buttonBar, cc.xyw(1, row, 3));
            builder.getPanel().setBackground(Color.WHITE);

            frame = new JFrame(getTitle());
            frame.setAlwaysOnTop(true);
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

    @SuppressWarnings("serial")
    private void initComponents() {
        addressBox = new JComboBox(SERVICE_PROVIDER_URLS);
        addressBox.setEditable(true);
        try {
            JTextField editorField = (JTextField) addressBox.getEditor()
                .getEditorComponent();
            editorField.setText("http://");
            editorField.setCaretPosition(editorField.getText().length());
        } catch (Exception e) {
            // Ignore
        }

        proxySettingsLabel = new ActionLabel(getController(),
            new AbstractAction(Translation
                .getTranslation("general.proxy_settings"))
            {
                public void actionPerformed(ActionEvent e) {
                    new HTTPProxySettingsDialog(getController(), frame).open();
                }
            }).getUIComponent();

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        infoLabel = SimpleComponentFactory.createLabel("X");
    }

    private String getTitle() {
        return Translation.getTranslation("config.loader.dialog.title");
    }

    private Component buildButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showProgressBar();
                new LoadingWorking().start();

            }
        });

        JButton skipButton = new JButton(Translation
            .getTranslation("config.loader.dialog.skip"));
        skipButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
                frame.dispose();
                mainProgrammContinue();
            }
        });
        JButton exitButton = new JButton(Translation
            .getTranslation("config.loader.dialog.exit"));
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        JComponent bar;
        if (getController().isStarted()) {
            skipButton.setText(Translation.getTranslation("general.cancel"));
            bar = ButtonBarFactory.buildCenteredBar(okButton, skipButton);
        } else {
            bar = ButtonBarFactory.buildCenteredBar(okButton, skipButton,
                exitButton);
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
        JButton okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
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

    private class LoadingWorking extends SwingWorker {
        @Override
        public Object construct() throws IOException {
            Properties preConfig = ConfigurationLoader
                .loadPreConfiguration((String) addressBox.getSelectedItem());
            if (!preConfig.isEmpty()
                && preConfig.containsKey(ConfigurationEntry.SERVER_HOST
                    .getConfigKey()))
            {
                ConfigurationLoader.mergeConfigs(preConfig, getController()
                    .getConfig(), true);
                // Seems to be valid, store.
                getController().saveConfig();
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
                    .getTranslation("config.loader.dialog.error.generic");
                if (t != null) {
                    if (t instanceof FileNotFoundException) {
                        errorMsg = Translation
                            .getTranslation("config.loader.dialog.error.config.notfound");
                    } else if (t instanceof MalformedURLException) {
                        errorMsg = Translation
                            .getTranslation("config.loader.dialog.error.address.invalid");
                    } else if (t instanceof IllegalArgumentException) {
                        errorMsg = Translation
                            .getTranslation("config.loader.dialog.error.address.invalid");
                    } else if (t instanceof UnknownHostException) {
                        errorMsg = Translation
                            .getTranslation("config.loader.dialog.error.host.notfound");
                    } else {
                        errorMsg = t.getMessage();
                    }
                }
            } else if (preConfig.size() == 0) {
                errorMsg = Translation
                    .getTranslation("config.loader.dialog.error.config.empty");
            } else if (!preConfig.containsKey(ConfigurationEntry.SERVER_HOST
                .getConfigKey()))
            {
                errorMsg = Translation
                    .getTranslation("config.loader.dialog.error.server.missing");
            }
            if (errorMsg != null) {
                showInfo(Translation.getTranslation(
                    "config.loader.dialog.error", errorMsg));
                return;
            }

            // Success
            showInfo(Translation.getTranslation("config.loader.dialog.loaded",
                String.valueOf(preConfig.size())));
            frame.setVisible(false);
            frame.dispose();
            mainProgrammContinue();

            if (finishedTrigger != null) {
                finishedTrigger.triggerCommit();
            }
            if (getController().isStarted()) {
                handleRestartRequest();
            }
        }

        /**
         * Asks user about restart and executes that if requested
         */
        private void handleRestartRequest() {
            int result = DialogFactory.genericDialog(getController(),
                Translation.getTranslation("preferences.dialog.restart.title"),
                Translation.getTranslation("preferences.dialog.restart.text"),
                new String[]{
                    Translation
                        .getTranslation("preferences.dialog.restart.restart"),
                    Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.QUESTION); // Default is restart

            if (result == 0) { // Restart
                getController().shutdownAndRequestRestart();
            }
        }
    }
}
