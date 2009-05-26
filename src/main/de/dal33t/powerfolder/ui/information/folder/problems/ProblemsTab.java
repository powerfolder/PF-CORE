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
* $Id: ProblemsTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.problems;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class ProblemsTab extends PFUIComponent {

    private JPanel uiComponent;
    private JScrollPane scrollPane;

    private MyOpenProblemAction openProblemAction;
    private MyClearProblemAction clearProblemAction;

    private FolderInfo folderInfo;
    private ProblemsTable problemsTable;
    private ProblemsTableModel problemsTableModel;
    private Problem selectedProblem;

    public ProblemsTab(Controller controller) {
        super(controller);
        problemsTableModel = new ProblemsTableModel(controller);
        problemsTable = new ProblemsTable(problemsTableModel);
        problemsTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        problemsTable.getSelectionModel().addListSelectionListener(
                new MySelectionListener());
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
            enableOnSelection();
        }
        return uiComponent;
    }

    private void initialize() {
        openProblemAction = new MyOpenProblemAction(getController());
        clearProblemAction = new MyClearProblemAction(getController());

        scrollPane = new JScrollPane(problemsTable);

        // Whitestrip
        UIUtil.whiteStripTable(problemsTable);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(scrollPane, cc.xy(2, 6));

        uiComponent = builder.getPanel();
    }

    private Component createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(openProblemAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(clearProblemAction));
        return bar.getPanel();

    }

    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
    }

    /**
     * Display problems.
     * 
     * @param problemList
     */
    public void updateProblems(List<Problem> problemList) {
        problemsTableModel.updateProblems(problemList);
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
        int selectedRow = problemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedProblem = (Problem) problemsTableModel.getValueAt(
                    problemsTable.getSelectedRow(), 0);
            openProblemAction.setEnabled(true);
        } else {
            selectedProblem = null;
            openProblemAction.setEnabled(false);
        }
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class MyOpenProblemAction extends BaseAction {

        private MyOpenProblemAction(Controller controller) {
            super("action_open_problem", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int selectedRow = problemsTable.getSelectedRow();
            Problem problem = (Problem) problemsTableModel.getValueAt(
                    selectedRow, 0);
            String wikiArticleURL = Help.getWikiArticleURL(getController(),
                    problem.getWikiLinkKey());
            try {
                BrowserLauncher.openURL(wikiArticleURL);
            } catch (IOException e1) {
                logSevere("IOException", e1);
            }

        }
    }

    private class MyClearProblemAction extends BaseAction {
        MyClearProblemAction(Controller controller) {
            super("action_clear_problem", controller);
        }

        public void actionPerformed(ActionEvent e) {
            Folder folder = getController().getFolderRepository().getFolder(folderInfo);
            if (folder != null) {
                if (selectedProblem == null) {
                    folder.removeAllProblems();
                } else {
                    folder.removeProblem(selectedProblem);
                }
            }
        }
    }

    /**
     * Class to detect table selection changes.
     */
    private class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableOnSelection();
        }
    }
}
