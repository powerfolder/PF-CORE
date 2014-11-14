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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import com.jgoodies.binding.value.Trigger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.preferences.HTTPProxySettingsDialog;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;

/**
 * #1784: For locking the user interface.
 *
 * @author sprajc
 */
public class UIUnLockDialog extends PFUIComponent {

    private JFrame frame;
    private JLabel serverLabel;
    private ActionLabel serverInfoLabel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComponent proxySettingsLabel;
    private JProgressBar progressBar;
    private JLabel infoLabel;
    private JButton okButton;
    private Object haltLock = new Object();
    private Trigger serverReloadTrigger;

    public UIUnLockDialog(Controller controller) {
        super(controller);
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

            FormLayout layout = new FormLayout("p, 3dlu, p:g",
                "p, 7dlu, p, 0, p, 3dlu, p, 3dlu, p, 7dlu, 12dlu, 14dlu, p");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.addLabel(Translation.getTranslation("exp.uilock.dialog.info"),
                cc.xyw(1, row, 3));
            row += 2;

            serverLabel.setBorder(Borders.createEmptyBorder("0, 0, 3dlu, 0"));
            builder.add(serverLabel, cc.xy(1, row));
            serverInfoLabel.getUIComponent().setBorder(
                Borders.createEmptyBorder("0, 0, 3dlu, 0"));
            builder.add(serverInfoLabel.getUIComponent(), cc.xy(3, row));

            row += 2;
            builder.addLabel(LoginUtil.getUsernameLabel(getController()),
                cc.xy(1, row));
            builder.add(usernameField, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("general.password") + ":", cc.xy(1, row));
            builder.add(passwordField, cc.xy(3, row));

            row += 2;
            builder.add(proxySettingsLabel, cc.xywh(3, row, 1, 1,
                "right, center"));

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

    @SuppressWarnings("serial")
    private void initComponents() {
        serverLabel = new JLabel(Translation.getTranslation("general.server"));

        serverReloadTrigger = new Trigger();
        serverReloadTrigger.addValueChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt) {
                serverInfoLabel.setText(ConfigurationEntry.SERVER_WEB_URL
                    .getValue(getController()));
            }
        });
        serverInfoLabel = new ActionLabel(getController(), new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) {
                new ConfigurationLoaderDialog(getController(),
                    serverReloadTrigger).openAndWait();
            }
        });
        serverInfoLabel.setText(ConfigurationEntry.SERVER_WEB_URL
            .getValue(getController()));

        usernameField = SimpleComponentFactory.createTextField(true);
        passwordField = SimpleComponentFactory.createPasswordField();
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
        infoLabel = SimpleComponentFactory.createLabel("");
    }

    private String getTitle() {
        return Translation.getTranslation("exp.uilock.dialog.title");
    }

    private Component buildButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showProgressBar();
                new LoadingWorking().execute();

            }
        });

        JButton helpButton = new JButton(
            Translation.getTranslation("exp.uilock.dialog.help"));
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BrowserLauncher.openURL(getController(),
                    Help.getWikiArticleURL(getController(), WikiLinks.UI_LOCK));
            }
        });
        JButton exitButton = new JButton(
            Translation.getTranslation("exp.uilock.dialog.exit"));
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });
        JComponent bar = ButtonBarFactory.buildCenteredBar(okButton,
            helpButton, exitButton);
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

    private class LoadingWorking extends SwingWorker<Boolean, Void> {

        @Override
        protected Boolean doInBackground() throws Exception {
            String username = usernameField.getText();
            char[] password = passwordField.getPassword();

            if (StringUtils.isBlank(username) || password == null
                || password.length == 0)
            {
                return false;
            }

            String url;
            if (getController().getOSClient() != null) {
                url = getController().getOSClient().getWebURL();
            } else {
                // Fallback
                url = ConfigurationEntry.SERVER_WEB_URL
                    .getValue(getController());
            }
            url += Constants.UI_LOCK_UNLOCK_URI;
            logWarning("Trying to unlock user interface at: " + url);
            url = LoginUtil.decorateURL(url, username, password);

            URL u = new URL(url);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.connect();

            int resCode = c.getResponseCode();
            if (resCode == 403) {
                // Unauthorized
                return false;
            } else if (resCode == 200) {
                // OK
                return true;
            }

            // Actually check response.
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            StreamUtils.copyToStream(c.getInputStream(), bOut);
            c.getInputStream().close();
            c.disconnect();
            String res = new String(bOut.toByteArray());
            return res.contains("ALLOWED_TO_UNLOCK_USER_INTERFACE");
        }

        @Override
        protected void done() {
            try {
                boolean unlocked = get();
                // Success
                if (unlocked) {
                    frame.setVisible(false);
                    frame.dispose();

                    // Switch login
                    String username = usernameField.getText();
                    char[] password = passwordField.getPassword();
                    getController().getOSClient().login(username, password);
                    LoginUtil.clear(password);
                    mainProgrammContinue();
                    return;
                } else {
                    showInfo(Translation
                        .getTranslation("exp.uilock.dialog.error.wronglogin"));
                }
            } catch (InterruptedException e) {
                logWarning(e);
            } catch (ExecutionException e) {
                logWarning(e);
                String msg = e.getCause() != null
                    && e.getCause().getMessage() != null ? e.getCause()
                    .getMessage() : e.toString();
                showInfo(msg);
            }
        }
    }
}
