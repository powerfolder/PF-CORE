package de.dal33t.powerfolder.ui.preferences;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.Proxy;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.net.HTTPProxySettings;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

public class HTTPProxySettingsDialog extends PFUIComponent {
    private Window parentFrame;
    private JDialog dialog;
    private JPanel panel;

    private JRadioButton directButton;
    private JRadioButton httpProxyButton;

    private JTextField proxyHostField;
    private JSpinner proxyPortField;
    private JCheckBox useUserAndPasswordBox;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel buttonBar;

    private ValueModel tempProxyHostModel;
    private ValueModel tempProxyUsernameModel;
    private ValueModel tempProxyPasswordModel;
    private ValueModel proxyTypeModel;

    public HTTPProxySettingsDialog(Controller controller) {
        this(controller, controller.getUIController().getMainFrame()
            .getUIComponent());
    }

    public HTTPProxySettingsDialog(Controller controller, Window parentFrame) {
        super(controller);
        this.parentFrame = parentFrame;
    }

    public void open() {
        if (dialog == null) {
            dialog = new JDialog(parentFrame, Translation
                .getTranslation("http.options.title"),
                ModalityType.APPLICATION_MODAL);
            dialog.setContentPane(getUIComponent());
            dialog.pack();
            dialog.setResizable(false);
            if (parentFrame != null) {
                int x = parentFrame.getX()
                    + (parentFrame.getWidth() - dialog.getWidth()) / 2;
                int y = parentFrame.getY()
                    + (parentFrame.getHeight() - dialog.getHeight()) / 2;
                dialog.setLocation(x, y);
            }
        }
        dialog.setVisible(true);
    }

    private JPanel getUIComponent() {
        if (panel == null) {
            initComponents();

            FormLayout layout = new FormLayout("r:p:grow, 3dlu, 80dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 14dlu, p");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(directButton, cc.xyw(1, row, 3));
            row += 2;
            builder.add(httpProxyButton, cc.xyw(1, row, 3));
            row += 2;
            builder.addLabel(Translation
                .getTranslation("http.options.host"), cc.xy(1, row));
            builder.add(proxyHostField, cc.xy(3, row));
            row += 2;
            builder.addLabel(Translation
                .getTranslation("http.options.port"), cc.xy(1, row));
            builder.add(proxyPortField, cc.xy(3, row));
            row += 2;
            builder.add(useUserAndPasswordBox, cc.xy(3, row));
            row += 2;
            builder.addLabel(Translation
                .getTranslation("http.options.username"), cc.xy(1, row));
            builder.add(proxyUsernameField, cc.xy(3, row));
            row += 2;
            builder.addLabel(Translation
                .getTranslation("http.options.password"), cc.xy(1, row));
            builder.add(proxyPasswordField, cc.xy(3, row));
            row += 2;
            builder.add(buttonBar, cc.xyw(1, row, 3));

            // builder.getPanel().setOpaque(false);
            return builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        Proxy.Type proxyType = StringUtils
            .isEmpty(ConfigurationEntry.HTTP_PROXY_HOST
                .getValue(getController()))
            ? Proxy.Type.DIRECT
            : Proxy.Type.HTTP;
        proxyTypeModel = new ValueHolder(proxyType, false);

        tempProxyHostModel = new ValueHolder(ConfigurationEntry.HTTP_PROXY_HOST
            .getValue(getController()), true);
        int proxyPort = ConfigurationEntry.HTTP_PROXY_PORT
            .getValueInt(getController());
        tempProxyUsernameModel = new ValueHolder(
            ConfigurationEntry.HTTP_PROXY_USERNAME.getValue(getController()),
            true);
        tempProxyPasswordModel = new ValueHolder(
            ConfigurationEntry.HTTP_PROXY_PASSWORD.getValue(getController()),
            true);

        // Proxy select
        directButton = BasicComponentFactory.createRadioButton(proxyTypeModel,
            Proxy.Type.DIRECT, Translation
                .getTranslation("http.options.directconnect"));
        directButton.setOpaque(false);

        httpProxyButton = BasicComponentFactory.createRadioButton(
            proxyTypeModel, Proxy.Type.HTTP, Translation
                .getTranslation("http.options.httpproxy"));
        httpProxyButton.setOpaque(false);

        // Proxy data

        proxyHostField = BasicComponentFactory
            .createTextField(tempProxyHostModel);
        proxyPortField = new JSpinner(new SpinnerNumberModel(proxyPort, 0,
            65536, 1));
        proxyPortField.setEditor(new JSpinner.NumberEditor(proxyPortField,
            "####0"));

        useUserAndPasswordBox = new JCheckBox(Translation
            .getTranslation("http.options.withauth"));
        useUserAndPasswordBox.setOpaque(false);
        useUserAndPasswordBox
            .setSelected(tempProxyUsernameModel.getValue() != null);

        proxyUsernameField = BasicComponentFactory
            .createTextField(tempProxyUsernameModel);
        proxyPasswordField = BasicComponentFactory
            .createPasswordField(tempProxyPasswordModel);

        okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
            .charAt(0));
        okButton.addActionListener(new OkAction());
        cancelButton = new JButton(Translation.getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation(
            "general.cancel.key").charAt(0));
        cancelButton.addActionListener(new CancelAction());

        buttonBar = ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
        buttonBar.setOpaque(false);

        updateComponents();
        initEventHandling();
    }

    private void initEventHandling() {
        useUserAndPasswordBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateComponents();
            }
        });
        proxyTypeModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateComponents();
            }
        });
    }

    private void updateComponents() {
        boolean useHttpProxy = Proxy.Type.HTTP
            .equals(proxyTypeModel.getValue());
        proxyHostField.setEditable(useHttpProxy);
        proxyPortField.setEnabled(useHttpProxy);
        useUserAndPasswordBox.setEnabled(useHttpProxy);
        proxyUsernameField.setEditable(useHttpProxy
            && useUserAndPasswordBox.isSelected());
        proxyPasswordField.setEditable(useHttpProxy
            && useUserAndPasswordBox.isSelected());
    }

    private void saveSettings() {
        boolean useHttpProxy = Proxy.Type.HTTP
            .equals(proxyTypeModel.getValue());
        boolean withAuth = useUserAndPasswordBox.isSelected();
        if (useHttpProxy) {
            String proxyUsername = withAuth
                ? proxyUsernameField.getText()
                : null;
            String proxyPassword = withAuth ? new String(proxyPasswordField
                .getPassword()) : null;
            HTTPProxySettings.saveToConfig(getController(), proxyHostField
                .getText(), (Integer) proxyPortField.getValue(), proxyUsername,
                proxyPassword);
        } else {
            HTTPProxySettings
                .saveToConfig(getController(), null, 0, null, null);
        }
        getController().saveConfig();
    }

    private class OkAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            saveSettings();
            dialog.setVisible(false);
            dialog.dispose();
        }

    }

    private class CancelAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }
}
