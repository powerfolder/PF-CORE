/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: DirectoryChooser.java 5178 2008-09-10 14:59:17Z harry $
 */
package de.dal33t.powerfolder.ui.dialog.directory;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class for choosing a directory. Shows a tree of the local file system. User
 * can select any non-hidden directory. Also displays the currently selected
 * directory. Also has a new subdirectory button for creating subdirectories.
 * NOTE: This class is package-private, not public, because it should only be
 * accessed through DirectoryChooser.
 */
public class DirectoryChooser extends BaseDialog {

    private List<Path> selectedDirs;
    private final DirectoryTree tree;
    private final JTextField pathField;
    private final JButton newDirButton;
    private DefaultTreeModel model;
    private JPopupMenu popupMenu;
    private NewDirectoryAction newDirectoryAction;
    private JButton okButton;
    private boolean multiSelect;

    /**
     * Constructor.
     *
     * @param controller
     *            for super class
     * @param initialValue
     *            the initial directory to start from
     * @param onlineFolders
     *            optional list of online folder names that are rendered as
     *            globe icons. These are expected to be online folders in the
     *            PF base dir that a user may want to create.
     * @param multiSelect
     *            whether multiple directories may be selected.
     */
    public DirectoryChooser(Controller controller, Path initialValue,
                     List<String> onlineFolders, boolean multiSelect) {
        super(Senior.NONE, controller, true);
        this.multiSelect = multiSelect;
        selectedDirs = new ArrayList<Path>();
        selectedDirs.add(initialValue);
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
        model = new DefaultTreeModel(rootTreeNode);
        tree = new DirectoryTree(model, onlineFolders);
        tree.getSelectionModel().setSelectionMode(multiSelect ?
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION :
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        pathField = new JTextField();
        newDirectoryAction = new NewDirectoryAction(getController());
        newDirectoryAction.setEnabled(false);
        newDirButton = new JButton(newDirectoryAction);
        tree.addMouseListener(new NavTreeListener());
    }

    public List<Path> getSelectedDirs() {
        if (selectedDirs == null) {
            return null;
        }
        return Collections.unmodifiableList(selectedDirs);
    }

    /**
     * ok and cancel buttons.
     *
     * @return
     */
    protected Component getButtonBar() {

        // Ok button sets the selected file path in the value model, if any
        // selected.
        okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                okEvent();
            }
        });

        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // Signal cancel with a null vm value.
                selectedDirs.clear();
                close();
            }
        });

        JButton useClassicButton = new JButton(Translation
            .getTranslation("dialog.directorychooser.use_classic"));
        useClassicButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                useClassicBrowser();
            }
        });

        return ButtonBarFactory.buildCenteredBar(useClassicButton, okButton,
            cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    private void okEvent() {

        // See if the user has just typed in a directory in the text area.
        if (selectedDirs.size() <= 1
            && StringUtils.isNotBlank(pathField.getText()))
        {
            if (selectedDirs.size() == 1) {
                Path selectedDir = selectedDirs.get(0);
                String selectedPath = selectedDir.toAbsolutePath().toString();
                String enteredPath = pathField.getText();
                if (!selectedPath.equals(enteredPath)) {
                    Path file = Paths.get(enteredPath);
                    selectedDirs.clear();
                    selectedDirs.add(file);
                }
            } else {
                String enteredPath = pathField.getText();
                Path file = Paths.get(enteredPath);
                selectedDirs.clear();
                selectedDirs.add(file);
            }
        }

        for (Path selectedDir : selectedDirs) {
            // Create any virtual folders now.
            if (Files.notExists(selectedDir)) {
                try {
                    Files.createDirectories(selectedDir);
                } catch (IOException e) {
                    logSevere("Failed to create directory", e);
                }
            }
        }

        if (!selectedDirs.isEmpty()) {
            close();
        }
    }

    protected JComponent getContent() {
        try {
            return getContent0();
        } catch (Exception e) {
            logSevere("Unable to render directory chooser. " + e, e);
            return new JLabel(Translation
                .getTranslation("dialog.directorychooser.error"));
        }
    }

    /**
     * Main content area. Shows a tree of the local file system and currently
     * selected path and new directory button.
     *
     * @return
     */
    private JComponent getContent0() {

        logFine("getContent()");
        // Populate root node with primary drives.
        Iterable<Path> fs = FileSystems.getDefault().getRootDirectories();

        FileSystemView fsv = FileSystemView.getFileSystemView();
        for (Path f : fs) {
            if (isFine()) {
                logFine("Root " + f);
            }

            DirectoryTreeNode treeNode = new DirectoryTreeNode(getController(),
                    fsv.getSystemDisplayName(f.toFile()), f, true, true);
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
        FormLayout layout = new FormLayout("pref, 3dlu, pref:grow, 3dlu, pref",
            "pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // Set the components.
        CellConstraints cc = new CellConstraints();
        int row = 1;
        builder.add(scrollPane, cc.xyw(1, row, 5));
        row += 2;
        if (multiSelect) {
            builder.add(new JLabel(Translation.getTranslation(
                    "dialog.directorychooser.hold_control")),
                    cc.xyw(1, row, 5));
            row +=2;
        }
        builder.add(
            new JLabel(Translation.getTranslation("general.directory")),
                cc.xy(1, row));
        builder.add(pathField, cc.xy(3, row));
        builder.add(ButtonBarFactory.buildRightAlignedBar(newDirButton), cc.xy(
            5, row));
        JComponent c = builder.getPanel();

        // Initialize the tree path on the path supplied.
        logFine("Initializing path...");
        tree.initializePath(selectedDirs.get(0));
        logFine("Initialized path");
        return c;
    }

    /**
     * Return an icon for dialog.
     *
     * @return
     */
    protected Icon getIcon() {
        return null;
    }

    /**
     * The page title
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.directory-chooser.title");
    }

    private void newDirectoryAction() {

        // Select the currently selected directory
        if (tree.getSelectionPath() == null
            || !(tree.getSelectionPath().getLastPathComponent() instanceof DirectoryTreeNode))
        {
            return;
        }
        DirectoryTreeNode dtn = (DirectoryTreeNode) tree.getSelectionPath()
            .getLastPathComponent();
        Path selected = dtn.getDir();
        String baseFile = selected.toAbsolutePath().toString();

        if (baseFile != null) {
            ValueModel subDirValueModel = new ValueHolder();
            NewDirectoryCreator ndc = new NewDirectoryCreator(getController(),
                true, baseFile, subDirValueModel);
            ndc.open();
            Object o = subDirValueModel.getValue();
            if (o != null) {
                String subDir = ((String) o).trim();
                if (subDir.length() > 0) {
                    Path f = Paths.get(baseFile, subDir);
                    if (Files.exists(f)) {
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("dialog.directorychooser.new.description"),
                                Translation.getTranslation(
                                    "dialog.directorychooser.new.exists", f
                                        .toAbsolutePath().toString()),
                                GenericDialogType.WARN);
                    } else {
                        boolean success = false;
                        try {
                            Files.createDirectories(f);
                            success = true;
                        } catch (IOException ioe) {
                            logInfo(ioe.getMessage());
                        }
                        if (success) {
                            TreePath selectionPath = tree.getSelectionPath();
                            Object parentComponent = selectionPath
                                .getLastPathComponent();
                            if (parentComponent instanceof DirectoryTreeNode) {

                                // Expand parent of new folder, so new child
                                // shows.
                                DirectoryTreeNode parentNode = (DirectoryTreeNode) parentComponent;
                                model.insertNodeInto(new DirectoryTreeNode(
                                        getController(), null, f, false, true),
                                        parentNode, parentNode.getChildCount());

                                // Find new folder in parent.
                                Enumeration children = parentNode.children();
                                while (children.hasMoreElements()) {
                                    Object node = children.nextElement();
                                    if (node instanceof DirectoryTreeNode) {
                                        DirectoryTreeNode childNode = (DirectoryTreeNode) node;
                                        if (childNode.getUserObject() instanceof File)
                                        {
                                            Path childFile = ((File) childNode
                                                .getUserObject()).toPath();
                                            if (childFile.equals(f)) {

                                                // Expand to child.
                                                TreeNode[] childPathNodes = model
                                                    .getPathToRoot(childNode);
                                                TreePath childPath = new TreePath(
                                                    childPathNodes);
                                                tree.setSelectionPath(childPath);
                                                tree.scrollPathToVisible(childPath);
                                                break;
                                            }
                                        } else {
                                            TreeNode[] pathNode = model.getPathToRoot(childNode);
                                            TreePath path = new TreePath(pathNode);
                                            tree.setSelectionPath(path);
                                            tree.scrollPathToVisible(path);
                                        }
                                    }
                                }
                            }
                        } else {
                            DialogFactory
                                .genericDialog(
                                    getController(),
                                    Translation
                                        .getTranslation("dialog.directorychooser.new.description"),
                                    Translation.getTranslation(
                                        "dialog.directorychooser.new.problem",
                                        f.toAbsolutePath().toString()),
                                    GenericDialogType.WARN);
                        }
                    }
                }
            }
        }
    }

    private void processTreeChange() {
        selectedDirs.clear();
        TreePath[] treePaths = tree.getSelectionPaths();
        if (treePaths != null && treePaths.length > 1) {
            // Multiple selection, so disable pathField.
            pathField.setEnabled(false);
            pathField.setText("");
            newDirectoryAction.setEnabled(false);
            for (TreePath treePath : treePaths) {
                DirectoryTreeNode dtn = (DirectoryTreeNode)
                        treePath.getLastPathComponent();
                if (isFine()) {
                    logFine("DirectoryTreeNode scanned " + dtn.isScanned()
                        + " volume " + dtn.isVolume());
                }
                Path f = dtn.getDir();
                if (isFine()) {
                    logFine("DirectoryTreeNode file " + f.toAbsolutePath());
                }
                selectedDirs.add(f);
            }
        } else {
            pathField.setEnabled(true);
            pathField.setText("");
            TreePath path = tree.getSelectionPath();
            if (path != null
                && path.getLastPathComponent() instanceof DirectoryTreeNode) {
                DirectoryTreeNode dtn = (DirectoryTreeNode) path
                    .getLastPathComponent();
                if (isFine()) {
                    logFine("DirectoryTreeNode scanned " + dtn.isScanned()
                        + " volume " + dtn.isVolume());
                }
                Path f = dtn.getDir();
                if (isFine()) {
                    logFine("DirectoryTreeNode file " + f.toAbsolutePath());
                }
                pathField.setText(f.toAbsolutePath().toString());
                newDirectoryAction.setEnabled(true);
                selectedDirs.add(f);
            }
        }
    }

    private void useClassicBrowser() {
        // We need to exit this dialog and get the DialogFactory to go again with a classic browser.
        selectedDirs = null; // Crude indication that we need to use classic.
        close();
    }

    /**
     * Selection listener to set text of path display field on selection change.
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
            super("action_directory_new", controller);
        }

        /**
         * Just fire the newDirectoryAction().
         *
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            newDirectoryAction();
        }
    }

    /**
     * Listener to respond to click events.
     */
    private class NavTreeListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {

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
