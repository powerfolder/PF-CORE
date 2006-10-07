/* $Id: WhatToDoPanel.java,v 1.13 2005/11/20 03:18:51 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The start panel of the "what to do" wizard line
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class WhatToDoPanel extends PFWizardPanel {
    static final int PICTO_FONT_SIZE = 6;
    // The options of this screen
    private static final Object syncPCsOption = new Object();
    private static final Object projectWorkOption = new Object();
    private static final Object filesharingOption = new Object();

    private boolean initalized = false;

    private JRadioButton syncPCsButton;
    private JRadioButton projectWorkButton;
    private JRadioButton filesharingButton;

    private JLabel documentationLink;

    private ValueModel decision;

    public WhatToDoPanel(Controller controller) {
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
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

        Object option = decision.getValue();
        if (option == syncPCsOption) {
            return new SyncPCsPanel(getController());
        } else if (option == projectWorkOption) {
            return new ProjectSyncPanel(getController());
        } else if (option == filesharingOption) {
            return new FilesharingPanel(getController());
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

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 0:g, pref, 0:g, pref, 20dlu",
            "5dlu, pref, 13dlu, top:127dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.whattodo.title")), cc.xywh(1, 2, 7, 1,
            CellConstraints.CENTER, CellConstraints.DEFAULT));

        builder.add(filesharingButton, cc.xy(2, 4));
        builder.add(syncPCsButton, cc.xy(4, 4));
        builder.add(projectWorkButton, cc.xy(6, 4));

        builder.add(documentationLink, cc.xywh(1, 5, 7, 1, "center, top"));

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

                Font defaultFont = PlasticLookAndFeel.getPlasticTheme()
                    .getUserTextFont();
                Font selectedFont = new Font(defaultFont.getFontName(),
                    Font.BOLD, defaultFont.getSize());
                Font unselectedFont = new Font(defaultFont.getFontName(), 0,
                    defaultFont.getSize());

                // Set all non-selection buttons to gray
                if (evt.getNewValue() == filesharingOption) {
                    filesharingButton.setIcon(Icons.FILESHARING_PICTO);
                    syncPCsButton.setIcon(Icons.SYNC_PCS_PICTO_GRAY);
                    projectWorkButton.setIcon(Icons.PROJECT_WORK_PICTO_GRAY);

                    filesharingButton.setFont(selectedFont);
                    syncPCsButton.setFont(unselectedFont);
                    projectWorkButton.setFont(unselectedFont);
                } else if (evt.getNewValue() == syncPCsOption) {
                    filesharingButton.setIcon(Icons.FILESHARING_PICTO_GRAY);
                    syncPCsButton.setIcon(Icons.SYNC_PCS_PICTO);
                    projectWorkButton.setIcon(Icons.PROJECT_WORK_PICTO_GRAY);

                    filesharingButton.setFont(unselectedFont);
                    syncPCsButton.setFont(selectedFont);
                    projectWorkButton.setFont(unselectedFont);
                } else if (evt.getNewValue() == projectWorkOption) {
                    filesharingButton.setIcon(Icons.FILESHARING_PICTO_GRAY);
                    syncPCsButton.setIcon(Icons.SYNC_PCS_PICTO_GRAY);
                    projectWorkButton.setIcon(Icons.PROJECT_WORK_PICTO);

                    filesharingButton.setFont(unselectedFont);
                    syncPCsButton.setFont(unselectedFont);
                    projectWorkButton.setFont(selectedFont);
                }
            }
        });

        filesharingButton = BasicComponentFactory.createRadioButton(decision,
            filesharingOption, Translation
                .getTranslation("wizard.whattodo.filesharing"));
        filesharingButton.setIcon(Icons.FILESHARING_PICTO);
        configurePictoButton(filesharingButton);

        syncPCsButton = BasicComponentFactory.createRadioButton(decision,
            syncPCsOption, Translation
                .getTranslation("wizard.whattodo.syncpcs"));
        syncPCsButton.setIcon(Icons.SYNC_PCS_PICTO);
        configurePictoButton(syncPCsButton);

        projectWorkButton = BasicComponentFactory.createRadioButton(decision,
            projectWorkOption, Translation
                .getTranslation("wizard.whattodo.projectwork"));
        projectWorkButton.setIcon(Icons.PROJECT_WORK_PICTO);
        configurePictoButton(projectWorkButton);

        documentationLink = Help.createHelpLinkLabel(Translation
            .getTranslation("wizard.whattodo.openonlinedocumentation"),
            "node/documentation");
        SimpleComponentFactory.setFontSize(documentationLink,
            PFWizard.HEADER_FONT_SIZE);
    }

    // Helper code ************************************************************

    /**
     * Configures a pictobutton
     * 
     * @param pictoButton
     */
    protected void configurePictoButton(JRadioButton pictoButton) {
        pictoButton.setOpaque(false);
        pictoButton.setBorderPainted(false);
        pictoButton.setHorizontalTextPosition(SwingConstants.CENTER);
        pictoButton.setVerticalTextPosition(SwingConstants.BOTTOM);
    }
}