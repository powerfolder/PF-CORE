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
 * $Id$
 */
package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.dialog.FileNameProblemDialog;
import de.dal33t.powerfolder.util.ui.UIPanel;

/**
 * This panel helps the translation team by displaying various dialogs.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.2 $
 */
public class DialogTestingPanel extends PFUIComponent implements UIPanel {

    private JPanel panel;

    public DialogTestingPanel(Controller controller) {
        super(controller);
    }

    public static String getTitle() {
        return "Dialog Testing";
    }

    public Component getUIComponent() {
        if (panel == null) {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            initComponents();
        }
        return panel;
    }

    /**
     * Add buttons to show variaous dialogs.
     */
    private void initComponents() {
        createFilenameProblemButton();
    }

    /**
     * Creates a button to display the FilenameProblemDialog. This needs at
     * least one folder to display problems for.
     */
    private void createFilenameProblemButton() {
        final FolderRepository folderRepository = getController()
            .getFolderRepository();
        JButton button;
        if (folderRepository.getFolders().length == 0) {
            button = new JButton(
                "Need some folders to display FileNameProblemDialog");
            button.setEnabled(false);
        } else {
            button = new JButton("FileNameProblemDialog");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Folder folder = folderRepository.getFolders()[0];
                    Map<FileInfo, List<FilenameProblem>> map = new HashMap<FileInfo, List<FilenameProblem>>();
                    FileInfo fileInfo = FileInfo.getTemplate(folder.getInfo(),
                        "test");
                    List<FilenameProblem> filenameProblems = new ArrayList<FilenameProblem>();
                    filenameProblems
                        .add(new FilenameProblem(fileInfo, fileInfo));
                    map.put(fileInfo, filenameProblems);
                    FileNameProblemDialog dialog = new FileNameProblemDialog(
                        getController(), new FileNameProblemEvent(folder, map));
                    dialog.openDialog();
                }
            });
        }
        panel.add(button);
    }
}