package de.dal33t.powerfolder.util.ui.directory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;

public class NewDirectoryCreator extends BaseDialog {

    private String baseDirectory;
    private final JTextField subdirField;
    private final ValueModel valueModel;

    public NewDirectoryCreator(Controller controller, boolean modal, String baseDirectory, ValueModel valueModel) {
        super(controller, modal);
        this.baseDirectory = baseDirectory;
        subdirField = new JTextField();
        this.valueModel = valueModel;
    }

    protected Component getButtonBar() {
        JButton okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                valueModel.setValue(subdirField.getText());
                setVisible(false);
            }
        });

        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected Component getContent() {

        // Panel builder.
        FormLayout layout = new FormLayout("pref, 4dlu, pref:grow", "pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // Set the components.
        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(Translation.getTranslation("dialog.newdirectorycreator.text",
                baseDirectory)).getPanel(), cc.xyw(1, 1, 3));
        builder.add(new JLabel(Translation.getTranslation("general.directory")), cc.xy(1, 3));
        builder.add(subdirField, cc.xy(3, 3));

        return builder.getPanel();
    }

    protected Icon getIcon() {
        // Half-size new-folder icon.
        return Icons.scaleIcon((ImageIcon) Icons.NEW_FOLDER, 0.5);
    }

    public String getTitle() {
        return Translation.getTranslation("dialog.newdirectorycreator.title");
    }
}
