package de.dal33t.powerfolder.util.ui.directory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Class for choosing a directory.
 * Shows a tree of the local file system.
 * User can select any non-hidden directory.
 * Also displayes the currently selected directory.
 * Also has a new subdirectory button for creating subdirectories.
 */
public class DirectoryChooser extends BaseDialog {

    private final ValueModel valueModel;
    private final DirectoryTree tree;
    private final JTextField pathField;
    private final JButton newDirButton;
    private DefaultTreeModel model;

    /**
     * Constructor.
     *
     * @param controller for super class
     * @param valueModel a value model with the existing directory path
     */
    public DirectoryChooser(Controller controller, ValueModel valueModel) {
        super(controller, true, true);
        this.valueModel = valueModel;
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
        model = new DefaultTreeModel(rootTreeNode);
        tree = new DirectoryTree(model);
        pathField = new JTextField();
        pathField.setEditable(false);
        newDirButton = new JButton(new NewDirectoryAction("dialog.directorychooser.new", getController()));
    }

    /**
     * ok and cancel buttons.
     *
     * @return
     */
    protected Component getButtonBar() {

        // Ok btton sets the selected file path in the value model, if any selected.
        JButton okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (tree.getSelectionPath() != null) {
                    Object o = tree.getSelectionPath().getLastPathComponent();
                    if (o instanceof DirectoryTreeNode) {
                        DirectoryTreeNode dtn = (DirectoryTreeNode) o;
                        File file = (File) dtn.getUserObject();
                        valueModel.setValue(file.getAbsolutePath());
                    }
                    setVisible(false);
                }
            }
        });

        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    /**
     * Main content area.
     * Shows a tree of the local file system
     * and currently selected path
     * and new directory button.
     *
     * @return
     */
    protected Component getContent() {

        // Populate root node with primary drives.
        File[] fs = File.listRoots();
        for (File f : fs) {
            DirectoryTreeNode treeNode = new DirectoryTreeNode(f, true);
            ((DefaultMutableTreeNode) tree.getModel().getRoot()).add(treeNode);
        }

        // Create tree in scroll pane
        tree.setCellRenderer(new DirectoryTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(new MyTreeSelectionListener());
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(450, 280));

        // Panel builder.
        FormLayout layout = new FormLayout("pref, 4dlu, pref:grow, 4dlu, pref",
            "pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // Set the components.
        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(
            Translation.getTranslation("dialog.directorychooser.text"))
            .getPanel(), cc.xyw(1, 1, 5));
        builder.add(scrollPane, cc.xyw(1, 3, 5));
        builder.add(
            new JLabel(Translation.getTranslation("general.directory")), cc.xy(
                1, 5));
        builder.add(pathField, cc.xy(3, 5));
        builder.add(ButtonBarFactory.buildRightAlignedBar(newDirButton), cc.xy(
            5, 5));
        Component c = builder.getPanel();

        // Initialize the tree path on the path supplied.
        tree.initializePath(new File((String) valueModel.getValue()));
        return c;
    }

    /**
     * Return an icon for dialog.
     *
     * @return
     */
    protected Icon getIcon() {
        return Icons.DIRECTORY;
    }

    /**
     * The page title
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.directorychooser.title");
    }

    private void newDirectoryAction() {

        // Select the currently selected directory
        if (tree.getSelectionPath() == null ||
                !(tree.getSelectionPath().getLastPathComponent()
                        instanceof DirectoryTreeNode)) {
            return;
        }
        DirectoryTreeNode dtn =
                (DirectoryTreeNode) tree.getSelectionPath().getLastPathComponent();
        File selectedDir = (File) dtn.getUserObject();
        String baseFile = selectedDir.getAbsolutePath();


        if (baseFile != null) {
            ValueModel subDirValueModel = new ValueHolder();
            NewDirectoryCreator ndc = new NewDirectoryCreator(getController(),
                    true, baseFile, subDirValueModel);
            ndc.open();
            Object o = subDirValueModel.getValue();
            if (o != null) {
                String subDir = (String) o;
                File f = new File(baseFile, subDir);
                if (f.exists()) {
                    DialogFactory.showWarningDialog(getUIComponent(),
                            Translation.getTranslation("dialog.directorychooser.new.description"),
                            Translation.getTranslation("dialog.directorychooser.new.exists", f.getAbsolutePath()));
                } else {
                    boolean success = f.mkdir();
                    if (success) {
                        TreePath selectionPath = tree.getSelectionPath();
                        Object pathComponent = selectionPath.getLastPathComponent();
                        if (pathComponent instanceof DirectoryTreeNode) {
                            DirectoryTreeNode node = (DirectoryTreeNode) pathComponent;
                            node.unscan();
                            model.insertNodeInto(new DirectoryTreeNode(f, false), node, node.getChildCount());
                            TreePath tp = new TreePath(node.getPath());
                            tree.expandPath(tp);
                        }
                    } else {
                        DialogFactory.showWarningDialog(getUIComponent(),
                                Translation.getTranslation("dialog.directorychooser.new.description"),
                                Translation.getTranslation("dialog.directorychooser.new.problem", f.getAbsolutePath()));
                    }
                }
            }
        }
    }
    /**
     * Selction listener to set text of path display field on selection change.
     */
    private class MyTreeSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            pathField.setText("");
            if (tree.getSelectionPath() != null &&
                    tree.getSelectionPath().getLastPathComponent() instanceof DirectoryTreeNode) {
                DirectoryTreeNode dtn = (DirectoryTreeNode) tree.getSelectionPath().getLastPathComponent();
                File f = (File) dtn.getUserObject();
                pathField.setText(f.getAbsolutePath());
            }
        }
    }

    private class NewDirectoryAction extends BaseAction {

        public NewDirectoryAction(String actionId, Controller controller) {
            super(actionId, controller);
        }

        public void actionPerformed(ActionEvent e) {
            newDirectoryAction();
        }
    }
}
