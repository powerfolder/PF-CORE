package de.dal33t.powerfolder.ui.information.folder.files.breadcrumb;

import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Reject;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FilesBreadcrumbPanel extends PFUIComponent {

    private JPanel breadcrumbPanel;
    private String root = "";

    public FilesBreadcrumbPanel(Controller controller) {
        super(controller);
        breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    }

    /**
     * Returns the breadcrumb panel.
     * 
     * @return
     */
    public JPanel getUiComponent() {
        return breadcrumbPanel;
    }

    /**
     * Sets the root (first) breadcrumb to the folder name.
     *
     * @param root
     */
    public void setRoot(String root) {
        Reject.ifNull(root, "Root is null");
        this.root = root;
        rebuild();
    }

    private void rebuild() {
        breadcrumbPanel.removeAll();
        breadcrumbPanel.add(new JLabel(root));
    }

    /**
     * Set the directory path that is being displayed.
     * 
     * @param folder
     * @param dir
     */
    public void setDirectory(Folder folder, DirectoryInfo dir) {
        setRoot(folder.getName());
        String[] filePathParts = dir.getRelativeName().split("/");
        for (String filePathPart : filePathParts) {
            breadcrumbPanel.add(new JLabel(">"));
            breadcrumbPanel.add(new JLabel(filePathPart));
        }
    }
}
