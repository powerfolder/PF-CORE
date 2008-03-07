package de.dal33t.powerfolder.util.ui.directory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.*;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Enumeration;

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
    private JPopupMenu popupMenu;
    private NewDirectoryAction newDirectoryAction;

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
        newDirectoryAction = new NewDirectoryAction(getController());
        newDirectoryAction.setEnabled(false);
        newDirButton = new JButton(newDirectoryAction);

        tree.addMouseListener(new NavTreeListener());
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
                okEvent();
            }
        });

        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    private void okEvent() {
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
        tree.addTreeExpansionListener(new MyTreeExpansionListener());
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
        return Icons.NEW_FOLDER;
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
                    DialogFactory.genericDialog(getController().getUIController()
                            .getMainFrame().getUIComponent(),
                            Translation.getTranslation(
                                    "dialog.directorychooser.new.description"),
                            Translation.getTranslation(
                                    "dialog.directorychooser.new.exists",
                                    f.getAbsolutePath()),
                            GenericDialogType.WARN);
                } else {
                    boolean success = f.mkdir();
                    if (success) {
                        TreePath selectionPath = tree.getSelectionPath();
                        Object parentComponent = selectionPath.getLastPathComponent();
                        if (parentComponent instanceof DirectoryTreeNode) {

                            // Expand parent of new folder, so new child shows.
                            DirectoryTreeNode parentNode =
                                    (DirectoryTreeNode) parentComponent;
                            model.insertNodeInto(new DirectoryTreeNode(f, false),
                                    parentNode, parentNode.getChildCount());

                            // Find new folder in parent.
                            Enumeration children = parentNode.children();
                            while (children.hasMoreElements()) {
                                Object node = children.nextElement();
                                if (node instanceof DirectoryTreeNode) {
                                    DirectoryTreeNode childNode = (DirectoryTreeNode) node;
                                    if (childNode.getUserObject() instanceof File) {
                                        File childFile = (File) childNode.getUserObject();
                                        if (childFile.equals(f)) {

                                            // Expand to child.
                                            TreeNode[] childPathNodes =
                                                    model.getPathToRoot(childNode);
                                            TreePath childPath = new TreePath(childPathNodes);
                                            tree.expandPath(childPath);
                                            tree.setSelectionPath(childPath);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        DialogFactory.genericDialog(getController().getUIController().getMainFrame().getUIComponent(),
                                Translation.getTranslation("dialog.directorychooser.new.description"),
                                Translation.getTranslation("dialog.directorychooser.new.problem", f.getAbsolutePath()),
                                GenericDialogType.WARN);
                    }
                }
            }
        }
    }

    private void processTreeChange() {
        pathField.setText("");
        if (tree.getSelectionPath() != null &&
                tree.getSelectionPath().getLastPathComponent() instanceof DirectoryTreeNode) {
            DirectoryTreeNode dtn = (DirectoryTreeNode) tree.getSelectionPath().getLastPathComponent();
            File f = (File) dtn.getUserObject();
            pathField.setText(f.getAbsolutePath());
            newDirectoryAction.setEnabled(tree.isExpanded(tree.getSelectionPath()) || dtn.isLeaf());
        }
    }
    
    /**
     * Selction listener to set text of path display field on selection change.
     */
    private class MyTreeSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            processTreeChange();
        }
    }

    /**
     * Action to fire newDirectoryAction()
     */
    private class NewDirectoryAction extends BaseAction {

        /**
         * Constructor
         */
        NewDirectoryAction(Controller controller) {
            super("dialog.directorychooser.new", controller);
        }

        /**
         * Just fire the newDirectoryAction().
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            newDirectoryAction();
        }
    }

    /**
     * Listener to respond to click / double click events.
     */
    private class NavTreeListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {

            if (e.getClickCount() == 2) {
                okEvent();
            }

            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            TreePath path = tree.getPathForLocation(evt.getX(), evt.getY());
            if (path == null) {
                return;
            }

            if (popupMenu == null) {
                popupMenu = new JPopupMenu();
                popupMenu.add(newDirectoryAction);
            }

            popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    private class MyTreeExpansionListener implements TreeExpansionListener {

        public void treeCollapsed(TreeExpansionEvent event) {
            processTreeChange();            
        }

        public void treeExpanded(TreeExpansionEvent event) {
            processTreeChange();
        }
    }
}
