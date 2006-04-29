/* $Id: ProjectNamePanel.java,v 1.5 2005/11/04 14:11:34 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

/**
 * Panel where user may choose the name of the project
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class ProjectNamePanel extends PFWizardPanel {
    private boolean initalized = false;

    private JTextField nameField;
    private ValueModel nameModel;

    public ProjectNamePanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return !StringUtils.isBlank((String) nameModel.getValue());
    }

    public boolean validateNext(List list) {
        return true;
    }

    public WizardPanel next() {
        // Create new folder info
        String name = "Project-" + nameField.getText();
        String folderId = "[" + IdGenerator.makeId() + "]";
        boolean secrect = true;
        FolderInfo folder = new FolderInfo(name, folderId, secrect);
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, folder);

        // Choose location...
        return new ChooseDiskLocationPanel(getController());
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

//        setBorder(new TitledBorder(Translation
//                .getTranslation("wizard.projectname.title"))); //Load invitation
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, 4dlu, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(this, layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
                .getTranslation("wizard.projectname.choose")), cc.xy(4, 2)); //Choose project name

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.addLabel(Translation
                .getTranslation("wizard.projectname.entername"), cc.xy(4, 4));//Enter the name of your project
        builder.add(nameField, cc.xy(4, 6));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        nameModel = new ValueHolder();

        nameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        nameField = BasicComponentFactory.createTextField(nameModel, false);
        // Ensure minimum dimension
        Util.ensureMinimumWidth(107, nameField);
    }
}