/* $Id: SyncPCsPanel.java,v 1.8 2005/06/12 22:57:10 totmacherr Exp $
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
import de.dal33t.powerfolder.util.Translation;

/**
 * Synchronize PCs main wizard panel
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class SyncPCsPanel extends PFWizardPanel {
    // The options of this screen
    private static final Object setupFolderOption = new Object();
    private static final Object joinFolderOption = new Object();

    private boolean initalized = false;

    private JRadioButton setupFolderButton;
    private JRadioButton joinFolderButton;

    private ValueModel decision;

    public SyncPCsPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return decision.getValue() != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    public WizardPanel next() {

        // INFO: New folder info created later in ChooseDiskLocation, because
        // name depend on selection directory

        // This is sync pcs profile !
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.SYNCHRONIZE_PCS);

        if (decision.getValue() == setupFolderOption) {
            // Setup choose disk location panel
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.syncpcspanel.select")); //Please
                                                                           // select
                                                                           // the
                                                                           // directory,
                                                                           // which
                                                                           // should
                                                                           // be
                                                                           // synced

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), //Setup
                                                                   // successfully
                Translation
                    .getTranslation("wizard.syncpcspanel.foldersyncsuccess") //You
                                                                             // successfully
                                                                             // set
                                                                             // up a
                                                                             // folder
                                                                             // synchronisation!\n
                    + Translation.getTranslation("wizard.syncpcspanel.pcsjoin")); //You
                                                                                  // are
                                                                                  // ready
                                                                                  // to
                                                                                  // join
                                                                                  // more
                                                                                  // PCs
                                                                                  // into
                                                                                  // this
                                                                                  // folder
                                                                                  // now
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            return new ChooseDiskLocationPanel(getController());
        } else if (decision.getValue() == joinFolderOption) {

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.syncpcspanel.selecttarget")); //Please
                                                                                 // select
                                                                                 // local
                                                                                 // target
                                                                                 // directory

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.syncpcspanel.folderjoinsuccess") //You
                                                                             // successfully
                                                                             // joined
                                                                             // a
                                                                             // folder
                                                                             // synchronisation!\n
                    + Translation.getTranslation("wizard.syncpcspanel.pcsjoin")); //You
                                                                                  // are
                                                                                  // ready
                                                                                  // to
                                                                                  // join
                                                                                  // more
                                                                                  // PCs
                                                                                  // into
                                                                                  // this
                                                                                  // folder
                                                                                  // now
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            return new LoadInvitationPanel(getController());
        }
        return null;
    }

    public boolean canFinish() {
        return false;
    }

    public boolean validateFinish(List list) {
        return true;
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

        //setBorder(new
        // TitledBorder(Translation.getTranslation("wizard.syncpcspanel.title")));
        // //Synchronize PCs
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout, this);

        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.syncpcspanel.title")), cc.xy(4, 2));

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.add(setupFolderButton, cc.xy(4, 4));
        builder.add(joinFolderButton, cc.xy(4, 5));

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
            setupFolderOption, Translation
                .getTranslation("wizard.syncpcspanel.setup")); //Setup a new
                                                               // synchronization
        setColors(setupFolderButton);

        joinFolderButton = BasicComponentFactory.createRadioButton(decision,
            joinFolderOption, Translation
                .getTranslation("wizard.syncpcspanel.takepart"));//Take part in
                                                                 // a
                                                                 // synchronisation
        setColors(joinFolderButton);

        // Set active picto label
        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.SYNC_PCS_PICTO);
    }
}