package de.dal33t.powerfolder.ui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

public class LoginWebServicePanel extends PFWizardPanel {
    private boolean initalized = false;

    private boolean folderSetupAfterwards;
    private ValueModel usernameModel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton;

    /**
     * @param controller
     * @param folderSetupAfterwards
     *            true if folder setup should shown after correct setup
     */
    public LoginWebServicePanel(Controller controller,
        boolean folderSetupAfterwards)
    {
        super(controller);
        this.folderSetupAfterwards = folderSetupAfterwards;
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return !StringUtils.isEmpty(usernameField.getText());
    }

    public boolean validateNext(List list) {
        // TODO Move this into worker. Make nicer
        boolean loginOk = getController().getWebServiceClient().checkLogin(
            usernameField.getText(), new String(passwordField.getPassword()));
        if (!loginOk) {
            list.add("Unable to login. Account data correct?");
        }
        ConfigurationEntry.WEBSERVICE_USERNAME.setValue(getController(),
            usernameField.getText());
        ConfigurationEntry.WEBSERVICE_PASSWORD.setValue(getController(),
            new String(passwordField.getPassword()));
        getController().saveConfig();
        return loginOk;
    }

    public WizardPanel next() {
        if (folderSetupAfterwards) {
            return new MirrorFolderPanel(getController());
        }
        return new TextPanelPanel(getController(),
            "Online Storage Login Successful",
            "The WebService is now correctly setup.\n"
                + "You may now start to backup Folders to it.");
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        setBorder(Borders.EMPTY_BORDER);
        FormLayout layout = new FormLayout(
            "pref, 15dlu, right:pref, 3dlu, fill:100dlu, 0:grow",
            "pref, 15dlu, pref, 7dlu, pref, 3dlu, pref, 7dlu, pref, 14dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout, this);
        builder.setBorder(Borders.createEmptyBorder("5dlu, 20dlu, 0, 0"));
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(new JLabel(Icons.WEBSERVICE_PICTO), cc.xywh(1, 3, row, 3,
            CellConstraints.DEFAULT, CellConstraints.TOP));
        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.webservice.login")), cc.xyw(3, row, 4));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.webservice.enteraccount"), cc
            .xyw(3, row, 4));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.webservice.username"), cc.xy(3, row));
        builder.add(usernameField, cc.xy(5, row));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.webservice.password"), cc.xy(3, row));
        builder.add(passwordField, cc.xy(5, row));

        row += 2;
        builder.add(registerButton, cc.xyw(3, row, 3));

        row += 2;
        LinkLabel link = new LinkLabel(Translation
            .getTranslation("wizard.webservice.learnmore"),
            "http://www.powerfolder.com/node/webservice");
        // FIXME This is a hack because of "Fusch!"
        link.setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
        builder.add(link, cc.xyw(3, row, 4));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        usernameModel = new ValueHolder(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()), true);
        usernameField = BasicComponentFactory.createTextField(usernameModel);
        passwordField = new JPasswordField(
            ConfigurationEntry.WEBSERVICE_PASSWORD.getValue(getController()));
        registerButton = new JButton(Translation
            .getTranslation("wizard.webservice.register"));
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BrowserLauncher.openURL(Constants.WEBSERVICE_REGISTER_URL);
                } catch (IOException e1) {
                    log().error(e1);
                }
            }
        });
        updateButtons();
        usernameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });
    }
}
