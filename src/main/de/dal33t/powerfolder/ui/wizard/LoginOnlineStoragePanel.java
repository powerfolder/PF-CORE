package de.dal33t.powerfolder.ui.wizard;

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
import static de.dal33t.powerfolder.disk.SyncProfile.AUTOMATIC_SYNCHRONIZATION;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import static de.dal33t.powerfolder.ui.wizard.PFWizard.SUCCESS_PANEL;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class LoginOnlineStoragePanel extends PFWizardPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton;
    private WizardPanel nextPanel;

    private boolean entryRequired;

    private ValueModel setupDefaultModel;
    private JCheckBox setupDefaultCB;

    private File defaultSynchronizedFolder;

    /**
     * @param controller
     * @param folderSetupAfterwards
     *            true if folder setup should shown after correct setup
     */
    public LoginOnlineStoragePanel(Controller controller,
        WizardPanel nextPanel, boolean entryRequired)
    {
        super(controller);
        this.nextPanel = nextPanel;
        this.entryRequired = entryRequired;
    }

    public boolean hasNext() {
        return !entryRequired || !StringUtils.isEmpty(usernameField.getText());
    }

    public boolean validateNext(List list) {
        if (!entryRequired && StringUtils.isEmpty(usernameField.getText())) {
            return true;
        }
        // TODO Move this into worker. Make nicer. Difficult because function returns loginOk.
        boolean loginOk = false;
        try {
            loginOk = getController().getOSClient().login(
                usernameField.getText(),
                new String(passwordField.getPassword())).isValid();
            if (!loginOk) {
                list.add(Translation
                    .getTranslation("online_storage.account_data"));
            }
            // FIXME Use separate account stores for diffrent servers?
            ConfigurationEntry.WEBSERVICE_USERNAME.setValue(getController(),
                usernameField.getText());
            ConfigurationEntry.WEBSERVICE_PASSWORD.setValue(getController(),
                new String(passwordField.getPassword()));
            getController().saveConfig();

        } catch (Exception e) {
            log().error("Problem logging in", e);
            list.add(Translation.getTranslation("online_storage.general_error",
                e.getMessage()));
        }
        return loginOk;
    }

    public WizardPanel next() {

        // Create default
        if ((Boolean) setupDefaultModel.getValue()) {

            // If there is already a default folder for this account, use that
            // for the name.
            FolderInfo accountFolder = getController().getOSClient().getAccount()
                    .getDefaultSynchronizedFolder();
            if (accountFolder != null) {
                defaultSynchronizedFolder = new File(getController()
                    .getFolderRepository().getFoldersBasedir(), accountFolder.name);
            }

            // Redirect via folder create of the deafult sync folder.
            FolderCreatePanel fcp = new FolderCreatePanel(getController());
            getWizardContext().setAttribute(CREATE_DESKTOP_SHORTCUT, false);
            getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                false);
            getWizardContext().setAttribute(SUCCESS_PANEL, nextPanel);
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                AUTOMATIC_SYNCHRONIZATION);
            getWizardContext().setAttribute(FOLDER_LOCAL_BASE,
                defaultSynchronizedFolder);
            getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
                true);

            return fcp;
        } else {
            return nextPanel;
        }
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("right:pref, 5dlu, pref, 0:grow",
            "pref, 10dlu, pref, 5dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.enteraccount"), cc.xyw(1, 1, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.username"), cc.xy(1, 3));
        builder.add(usernameField, cc.xy(3, 3));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.password"), cc.xy(1, 5));
        builder.add(passwordField, cc.xy(3, 5));

        builder.add(registerButton, cc.xy(3, 7));

        LinkLabel link = new LinkLabel(Translation
            .getTranslation("wizard.webservice.learnmore"),
            "http://www.powerfolder.com/node/webservice");
        // FIXME This is a hack because of "Fusch!"
        link.setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
        builder.add(link, cc.xyw(1, 9, 4));

        if (defaultSynchronizedFolder.exists()) {
            // Hmmm. User has already created this???
            setupDefaultCB.setSelected(false);
        } else {
            builder.add(createSetupDefultPanel(), cc.xyw(1, 11, 4));
        }

        return builder.getPanel();
    }

    private Component createSetupDefultPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(setupDefaultCB, cc.xy(1, 1));
        builder.add(Help.createWikiLinkLabel("Default_Folder"), cc.xy(
            3, 1));
        builder.setOpaque(true);
        builder.setBackground(Color.white);

        return builder.getPanel();
    }

    // UI building ************************************************************

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        // FIXME Use separate account stores for diffrent servers?
        ValueModel usernameModel = new ValueHolder(
            ConfigurationEntry.WEBSERVICE_USERNAME.getValue(getController()),
            true);
        usernameField = BasicComponentFactory.createTextField(usernameModel);
        passwordField = new JPasswordField(
            ConfigurationEntry.WEBSERVICE_PASSWORD.getValue(getController()));
        registerButton = new JButton(Translation
            .getTranslation("wizard.webservice.register"));
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    BrowserLauncher
                        .openURL(Constants.ONLINE_STORAGE_REGISTER_URL);
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

        setupDefaultModel = new ValueHolder(true);
        setupDefaultCB = BasicComponentFactory.createCheckBox(
            setupDefaultModel, Translation
                .getTranslation("wizard.login_online_storage.setup_default"));
        setupDefaultCB.setOpaque(true);
        setupDefaultCB.setBackground(Color.white);

        defaultSynchronizedFolder = new File(getController()
            .getFolderRepository().getFoldersBasedir(), Translation
            .getTranslation("wizard.basicsetup.default_folder_name"));
    }

    protected Icon getPicto() {
        return Icons.WEBSERVICE_PICTO;
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.webservice.login");
    }
}
