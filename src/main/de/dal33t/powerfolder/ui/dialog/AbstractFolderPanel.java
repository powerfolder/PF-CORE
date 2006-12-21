/* $Id: AbstractFolderPanel.java,v 1.13 2006/02/04 13:33:01 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.render.PFListCellRenderer;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;

/**
 * A general supertype for all panels displaying/editing the folder properties
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public abstract class AbstractFolderPanel extends BaseDialog {
    private FolderInfo fInfo;

    private JButton okButton;
    private JButton cancelButton;

    private JTextField nameField;
    private SyncProfileSelectionBox profileBox;
    private JComponent baseDirSelectionField;

    private ValueModel nameModel;
    private ValueModel baseDirModel;

    /**
     * Constructs a new preferences panel for a folder, folder may be empty
     * 
     * @param controller
     * @param fInfo
     */
    public AbstractFolderPanel(Controller controller, FolderInfo fInfo) {
        super(controller, true);
        this.fInfo = fInfo;
    }

    // Abstract interface methods *********************************************

    /**
     * Method should return the message, which is displayed ontop of settings
     * 
     * @return the info text
     */
    protected abstract String getMessage();

    /**
     * Executed when ok is pressed
     */
    protected abstract void okPressed();

    /**
     * Executes when cancle is pressed
     */
    protected abstract void cancelPressed();

    /**
     * Enables/Disables name field editing
     * 
     * @param enabled
     */
    protected void setNameEditable(boolean enabled) {
        if (nameField != null) {
            nameField.setEditable(enabled);
        }
    }

    // Exposing ***************************************************************

    public final FolderInfo getFolderInfo() {
        return fInfo;
    }

    protected final void setFolderInfo(FolderInfo afInfo) {
        fInfo = afInfo;
    }

    protected final ValueModel getNameModel() {
        return nameModel;
    }

    protected final ValueModel getBaseDirModel() {
        return baseDirModel;
    }

    protected final SyncProfile getSelectedSyncProfile() {
        return profileBox.getSelectedSyncProfile();
    }

    // UI Methods *************************************************************

    protected void initCustomComponents() {
    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        Folder folder = fInfo != null ? fInfo.getFolder(getController()) : null;

        nameModel = new ValueHolder(fInfo != null ? fInfo.name : "");

        nameField = BasicComponentFactory.createTextField(nameModel);

        // Sync profile select box
        profileBox = new SyncProfileSelectionBox(folder != null ? folder
            .getSyncProfile() : null);
        profileBox.setRenderer(new PFListCellRenderer());

        // Base dir selection
        baseDirModel = new ValueHolder();
        baseDirSelectionField = ComplexComponentFactory
            .createFolderBaseDirSelectionField(nameModel, baseDirModel,
                getController());

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });
        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        initCustomComponents();
    }

    // Methods for BaseDialog *************************************************

    /**
     * Gets the additional custom components from an folder panel.
     * 
     * @param firstColumnSpec
     *            the specs of the first column of the parent panel.
     * @return the component to display
     */
    protected abstract JComponent getCustomComponents(String firstColumnSpec);

    protected final Component getContent() {
        // Initalize components
        initComponents();

        String firstColumnSpec = "right:80dlu";
        String columnSpecs = firstColumnSpec + ", 4dlu, max(140dlu;pref):grow";
        FormLayout layout = new FormLayout(columnSpecs,
            "pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, fill:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("0, 0, 20dlu, 0"));

        CellConstraints cc = new CellConstraints();
        int row = 1;
        builder.addLabel(getMessage(), cc.xywh(1, row, 3, 1));

        row += 2;
        builder.addLabel(Translation.getTranslation("general.foldername"), cc
            .xy(1, row));
        builder.add(nameField, cc.xy(3, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("general.synchonisation"),
            cc.xy(1, row));
        builder.add(Help.addHelpLabel(profileBox), cc.xy(3, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("general.localcopyat"), cc
            .xy(1, row));
        builder.add(baseDirSelectionField, cc.xy(3, row));

        row += 2;
        builder.add(getCustomComponents(firstColumnSpec), cc.xyw(1, row, 3));

        return builder.getPanel();
    }

    protected final Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected final JButton getOkButton() {
        return okButton;
    }

    protected final JButton getCancelButton() {
        return cancelButton;
    }
}