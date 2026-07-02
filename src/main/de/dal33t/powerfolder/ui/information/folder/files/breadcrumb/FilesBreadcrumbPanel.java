/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.information.folder.files.breadcrumb;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.information.folder.files.FilesTab;
import de.dal33t.powerfolder.ui.widget.ActionLabel;

public class FilesBreadcrumbPanel extends PFUIComponent {

    private final FilesTab parent;
    private JPanel breadcrumbPanel;

    public FilesBreadcrumbPanel(Controller controller, FilesTab parent) {
        super(controller);
        this.parent = parent;
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
     * @param folderInfo
     */
    public void setRoot(FolderInfo folderInfo) {
        rebuild(folderInfo, new String[0]);
    }

    /**
     * Set the directory path that is being displayed.
     *
     * @param folderInfo
     * @param dir
     */
    public void setDirectory(FolderInfo folderInfo, DirectoryInfo dir) {
        String[] filePathParts = dir.getRelativeName().split("/");
        rebuild(folderInfo, filePathParts);
    }

    private void rebuild(FolderInfo folderInfo, String[] filePathParts) {
        breadcrumbPanel.removeAll();
        Action rootAction = new AbstractAction(folderInfo.getLocalizedName()) {
            public void actionPerformed(ActionEvent e) {
                parent.selectionChanged("");
            }
        };
        breadcrumbPanel.add(new ActionLabel(getController(), rootAction).getUIComponent());

        StringBuilder sb = new StringBuilder();
        for (String filePathPart : filePathParts) {
            if (filePathPart.length() > 0) {
                breadcrumbPanel.add(new JLabel(">"));
                sb.append(filePathPart);
                final String s = sb.toString();
                Action partAction = new AbstractAction(filePathPart) {
                    public void actionPerformed(ActionEvent e) {
                        parent.selectionChanged(s);
                    }
                };
                breadcrumbPanel.add(new ActionLabel(getController(), partAction).getUIComponent());
                sb.append('/');
            }
        }
    }

}
