package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.model.NodesSelectTableModel;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * Dialog for selecting a number of users for the invite wizard.
 */
public class NodesSelectDialog extends PFUIComponent {

    private ValueModel viaPowerFolderModel;
    private Collection<Member> viaPowerFolderMembers;

    private JDialog uiComponent;
    private NodesSelectTable nodesSelectTable;
    private NodesSelectTableModel nodesSelectTableModel;
    private JCheckBox hideOffline;

    /**
     * Initialize
     *
     * @param controller
     * @param viaPowerFolderModel
     * @param viaPowerFolderMembers
     */
    public NodesSelectDialog(Controller controller, ValueModel viaPowerFolderModel, Collection<Member> viaPowerFolderMembers) {
        super(controller);
        this.viaPowerFolderModel = viaPowerFolderModel;
        this.viaPowerFolderMembers = viaPowerFolderMembers;
    }

    /**
     * Initalizes / builds all ui elements
     */
    public void initComponents() {

        // General dialog initalization
        uiComponent = new JDialog(getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation("dialog.user_select.title"), true);

        uiComponent.setResizable(false);
        uiComponent.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JButton okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key").charAt(0));
        JButton cancelButton = new JButton(Translation.getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation("general.cancel.key").charAt(0));
        JComponent buttonBar = ButtonBarFactory.buildCenteredBar(okButton, cancelButton);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        // OK is the default
        uiComponent.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new MyOkListener());

        // Layout
        FormLayout layout = new FormLayout(
            "pref:grow",
            "pref, 14dlu, pref, 14dlu, pref, 14dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.DLU14_BORDER);
        CellConstraints cc = new CellConstraints();

        // Add components
        builder.addLabel(Translation.getTranslation("dialog.user_select.text"),
                cc.xy(1, 1));

        hideOffline = new JCheckBox(Translation
            .getTranslation("hideoffline.name"));
        builder.add(hideOffline, cc.xy(1, 3));
        hideOffline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doHide();
            }
        });
        nodesSelectTableModel = new NodesSelectTableModel(getController());
        nodesSelectTable = new NodesSelectTable(nodesSelectTableModel);

        // Autoselect row if there is only one member.
        if (nodesSelectTableModel.getRowCount() == 1) {
            nodesSelectTable.setRowSelectionInterval(0, 0);
        }
        JScrollPane pane = new JScrollPane(nodesSelectTable);
        pane.setPreferredSize(new Dimension(400, 200));

        builder.add(pane, cc.xy(1, 5));
        
        builder.add(buttonBar, cc.xy(1, 7));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.pack();

        // Orientation
        Component parent = uiComponent.getOwner();
        if (parent != null) {
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;
            uiComponent.setLocation(x, y);
        }
    }

    /**
     * Hide / show offline users.
     */
    private void doHide() {
        nodesSelectTableModel.setHideOffline(hideOffline.isSelected());
    }

    /**
     * Returns the ui component (dialog)
     *
     * @return
     */
    private JDialog getUIComponent() {
        if (uiComponent == null) {
            initComponents();
        }
        return uiComponent;
    }

    /**
     * Opens the dialog
     *
     * @return not used
     */
    public boolean open() {
        log().warn("Opening download dialog");
        getUIComponent().setVisible(true);
        return true;
    }

    /**
     * Closes the dialog
     */
    public void close() {
        if (uiComponent != null) {
            uiComponent.dispose();
        }
    }

    /**
     * Confirmation button action.
     */
    private class MyOkListener implements ActionListener {

        /**
         * Set the value model and user collection in the underlying wizard.
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            Collection<Member> selectedMembers = nodesSelectTable.getSelectedMembers();
            if (selectedMembers.isEmpty()) {
                viaPowerFolderModel.setValue(Translation.getTranslation("send_invitation.no_users"));
            } else if (selectedMembers.size() == 1) {
                viaPowerFolderModel.setValue(selectedMembers.iterator().next().getNick());
            } else {
                viaPowerFolderModel.setValue(Translation.getTranslation("send_invitation.multi_users"));
            }
            viaPowerFolderMembers.clear();
            viaPowerFolderMembers.addAll(selectedMembers);
            close();
        }
    }
}
