/* $Id: AbstractFolderPanel.java,v 1.13 2006/02/04 13:33:01 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
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
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public abstract class AbstractFolderPanel extends BaseDialog {
    private FolderInfo fInfo;

    private JButton okButton;
    private JButton cancelButton;

    private JTextField nameField;
    private JRadioButton publicButton;
    private JRadioButton secretButton;
    private SyncProfileSelectionBox profileBox;
    private JComponent baseDirSelectionField;

    private ValueModel nameModel;
    private ValueModel secretModel;
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

    protected final FolderInfo getFolderInfo() {
        return fInfo;
    }

    protected final ValueModel getNameModel() {
        return nameModel;
    }

    protected final ValueModel getSecrectModel() {
        return secretModel;
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
     * 
     * @return
     */
    private void initComponents() {
        Folder folder = fInfo != null ? fInfo.getFolder(getController()) : null;

        nameModel = new ValueHolder(fInfo != null ? fInfo.name : "");

        nameField = BasicComponentFactory.createTextField(nameModel);

        // setup secrect model
        secretModel = new ValueHolder(fInfo != null ? Boolean
            .valueOf(fInfo.secret) : Boolean.valueOf(!getController()
            .isPublicNetworking()));

        publicButton = BasicComponentFactory.createRadioButton(secretModel,
            Boolean.FALSE, Translation.getTranslation("general.folder.public"));
        secretButton = BasicComponentFactory.createRadioButton(secretModel,
            Boolean.TRUE, Translation.getTranslation("general.folder.secret"));

        if (!getController().isPublicNetworking()) {
            // Disable selection possibility in private mode
            publicButton.setEnabled(false);
            secretButton.setEnabled(false);
        }

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
     * @return Custom row specifications for a FormLayout
     *          (including a succeeding ", " if it is not empty)
     */
    protected String getCustomRows() {
        return "";        
    }
    
    /**
     * Append custom created components to the panel using the given builder
     * @param builder Builder used to create the panel
     * @param row the last row added
     * @return the last row used by the custom components
     */
    protected int appendCustomComponents(@SuppressWarnings("unused") PanelBuilder builder, 
        @SuppressWarnings("unused") int row) {
        return row;
    }

    protected final Component getContent() {
        // Initalize components
        initComponents();

        FormLayout layout = new FormLayout(
            "right:pref, 7dlu, max(140dlu;pref):grow",
            "pref, 14dlu, pref, 7dlu, pref, 3dlu, pref, 7dlu, pref, 3dlu, pref, "
                + getCustomRows() + "14dlu, pref, 3dlu, pref, 10dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.addLabel(getMessage(), cc.xywh(1, 1, 3, 1));
        builder.addLabel(" ", cc.xywh(1, 3, 3, 1));

        builder.addLabel(Translation.getTranslation("general.foldername"), cc
            .xy(1, 3));
        builder.add(nameField, cc.xy(3, 3));

        builder.addLabel(Translation.getTranslation("general.visibility"), cc
            .xy(1, 5));
        builder.add(publicButton, cc.xy(3, 5));
        builder.add(secretButton, cc.xy(3, 7));

        builder.addLabel(Translation.getTranslation("general.synchonisation"),
            cc.xy(1, 9));
        builder.add(Help.addHelpLabel(profileBox), cc.xy(3, 9));

        builder.addLabel(Translation.getTranslation("general.localcopyat"), cc
            .xy(1, 11));
        builder.add(baseDirSelectionField, cc.xy(3, 11));

        
        int row = appendCustomComponents(builder, 11);
        
        // builder.addLabel(Translation.getTranslation("general.datawarning.0"),
        // cc.xy(1, 13));
        row += 2;
        builder.addLabel(Translation.getTranslation("general.datawarning.1"),
            cc.xywh(1, row, 3, 1, "center, center"));
        row += 2;
        builder.addLabel(Translation.getTranslation("general.datawarning.2"),
            cc.xywh(1, row, 3, 1, "center, center"));
        
        return builder.getPanel();
    }

    protected final Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }
}