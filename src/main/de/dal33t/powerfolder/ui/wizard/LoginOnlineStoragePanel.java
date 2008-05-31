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
import de.dal33t.powerfolder.disk.Folder;
import static de.dal33t.powerfolder.disk.SyncProfile.AUTOMATIC_SYNCHRONIZATION;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.Icons;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_LOCAL_BASE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BASIC_SETUP_ATTIRBUTE;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.File;
import java.util.List;

public class LoginOnlineStoragePanel extends PFWizardPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton registerButton;
    private WizardPanel nextPanel;

    private boolean entryRequired;

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

            // Configure default folder for OS.
            // If come from FolderCreate of default folder in basic mode.
            Object object = getWizardContext().getAttribute(BASIC_SETUP_ATTIRBUTE);
            if (object instanceof Boolean && (Boolean) object)
            {
                object = getWizardContext().getAttribute(FOLDER_LOCAL_BASE);
                if (object != null && object instanceof File)
                {
                    File defaultSynchronizedFolder = (File) object;
                    if (defaultSynchronizedFolder.exists())
                    {
                        for (Folder folder : getController()
                                .getFolderRepository().getFolders())
                        {
                            if (folder.getLocalBase()
                                    .equals(defaultSynchronizedFolder))
                            {
                                getController().getOSClient().getAccount()
                                        .setDefaultSynchronizedFolder(folder
                                                .getInfo());
                                getController().getOSClient().getFolderService()
                                        .createFolder(folder.getInfo(),
                                                AUTOMATIC_SYNCHRONIZATION);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log().error("Problem logging in", e);
            list.add(Translation.getTranslation("online_storage.general_error",
                e.getMessage()));
        }
        return loginOk;
    }

    public WizardPanel next() {
        return nextPanel;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("right:pref, 5dlu, pref, 0:grow",
            "pref, 10dlu, pref, 5dlu, pref, 5dlu, pref, 15dlu, pref");
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
    }

    protected Icon getPicto() {
        return Icons.WEBSERVICE_PICTO;
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.webservice.login");
    }
}
