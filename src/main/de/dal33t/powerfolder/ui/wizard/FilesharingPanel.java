/* $Id: FilesharingPanel.java,v 1.6 2005/11/20 03:18:23 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.FolderCreatePanel;
import de.dal33t.powerfolder.util.Translation;

/**
 * The start panel for the filesharing options in wizard
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FilesharingPanel extends PFWizardPanel {

    // The options of this screen
    private static final Object shareFolderOption = new Object();
    private static final Object loadInvitationOption = new Object();
    private static final Object browseFoldersOption = new Object();

    private boolean initalized = false;

    private JRadioButton setupFolderButton;
    private JRadioButton loadInvitationButton;
    private JRadioButton browseFoldersButton;

    private ValueModel decision;

    public FilesharingPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return decision.getValue() != browseFoldersOption
            && decision.getValue() != null;
    }

    public boolean validateNext(List list) {
        if (decision.getValue() == shareFolderOption) {
            FolderCreatePanel panel = new FolderCreatePanel(getController());
            panel.open();
            return panel.folderCreated();
        }
        return true;
    }

    public WizardPanel next() {
        if (decision.getValue() == shareFolderOption) {
            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.filesharing.sharedsuccess"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            // Show success directly
            return successPanel;
        } else if (decision.getValue() == loadInvitationOption) {
            // Hmmm, use auto-download from friends !
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.AUTO_DOWNLOAD_FROM_FRIENDS);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.filesharing.selecttarget")); // Please
            // select
            // local
            // target
            // directory

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.filesharing.folderjoinsuccess") // You
            // successfully
            // joined
            // a
            // folder
            // synchronisation!\n
            );
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            return new LoadInvitationPanel(getController());
        }
        return null;
    }

    public boolean canFinish() {
        return decision.getValue() == browseFoldersOption;
    }

    public boolean validateFinish(List list) {
        return true;
    }

    public void finish() {
        if (decision.getValue() == browseFoldersOption) {
            // View switches to public folders preview, No success screen
            // TODO OMG find a better way
            getController().getUIController().getControlQuarter().setSelected(
                getController().getUIController().getFolderRepositoryModel()
                    .getPublicFoldersTreeNode());
        }
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        // setBorder(new TitledBorder(Translation
        // .getTranslation("wizard.filesharing.title")));//Filesharing
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, pref, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.filesharing.title")), cc.xy(4, 2)); // Filesharing

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.add(setupFolderButton, cc.xy(4, 4));
        builder.add(loadInvitationButton, cc.xy(4, 5));
        builder.add(browseFoldersButton, cc.xy(4, 6));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        decision = new ValueHolder();

        // Behavior
        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        setupFolderButton = BasicComponentFactory.createRadioButton(decision,
            shareFolderOption, Translation
                .getTranslation("wizard.filesharing.share"));// Share/Open a
        // new
        // foder
        setColors(setupFolderButton);

        loadInvitationButton = BasicComponentFactory.createRadioButton(
            decision, loadInvitationOption, Translation
                .getTranslation("wizard.filesharing.loadinvitation"));
        setColors(loadInvitationButton);

        browseFoldersButton = BasicComponentFactory.createRadioButton(decision,
            browseFoldersOption, Translation
                .getTranslation("wizard.filesharing.browse")); // Browse/Join
        // folders
        setColors(browseFoldersButton);
        // Only enabled when public networking
        browseFoldersButton.setEnabled(getController().isPublicNetworking());

        // Set active picto label
        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILESHARING_PICTO);
    }
}