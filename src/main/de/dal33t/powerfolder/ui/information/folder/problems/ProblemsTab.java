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
 *
 */
package de.dal33t.powerfolder.ui.information.folder.problems;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ResolvableProblem;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.StringUtils;

public class ProblemsTab extends PFUIComponent {

    private JPanel uiComponent;
    private JScrollPane scrollPane;

    private MyOpenProblemAction openProblemAction;
    private MyResolveProblemAction resolveProblemAction;
    private MyCleanupProblemAction cleanupProblemAction;

    private FolderInfo folderInfo;
    private final ProblemsTable problemsTable;
    private final ProblemsTableModel problemsTableModel;
    private Problem selectedProblem;

    public ProblemsTab(Controller controller) {
        super(controller);
        problemsTableModel = new ProblemsTableModel(controller);
        problemsTable = new ProblemsTable(problemsTableModel, controller);
        // PFC-3657: the leading check-box column is how several problems are
        // picked to repair at once; row selection stays single (Open/double-click).
        problemsTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        problemsTable.getSelectionModel().addListSelectionListener(
            new MySelectionListener());
        // Ticking boxes must also refresh the Resolve button state.
        problemsTableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (uiComponent != null) {
                    enableOnSelection();
                }
            }
        });
        problemsTable.addMouseListener(new TableMouseListener());
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
        resolveProblemAction = new MyResolveProblemAction(getController());
        cleanupProblemAction = new MyCleanupProblemAction(getController());

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

        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            JButton openBtn = new JButton(openProblemAction);
            openBtn.setIcon(null);
            bar.addGridded(openBtn);
            bar.addRelatedGap();
            bar.addRelatedGap();
        }

        JButton resolveBtn = new JButton(resolveProblemAction);
        resolveBtn.setIcon(null);
        bar.addGridded(resolveBtn);
        bar.addRelatedGap();

        JButton cleanupBtn = new JButton(cleanupProblemAction);
        cleanupBtn.setIcon(null);
        bar.addGridded(cleanupBtn);

        return bar.getPanel();
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
        selectedProblem = null;
        if (problemsTableModel.getRowCount() > 0) {
            problemsTable.getSelectionModel().setSelectionInterval(0, 0);
        } else {
            problemsTable.getSelectionModel().removeIndexInterval(0,
                problemsTableModel.getRowCount());
        }
    }

    /**
     * Display problems.
     * 
     * @param problemList
     */
    public void updateProblems(List<Problem> problemList) {
        problemsTableModel.updateProblems(problemList);
        enableOnSelection();

        if (getUIController().isShowingFolder()
            && !ConfigurationEntry.FILES_ENABLED
                .getValueBoolean(getController()) && problemList.isEmpty())
        {
            getUIController().getMainFrame().hideInlineInfoPanel();;
        }
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
        getUIComponent();
        int selectedRow = problemsTable.getSelectedRow();
        selectedProblem = problemsTableModel.getProblemAt(selectedRow);
        openProblemAction.setEnabled(selectedProblem != null
            && selectedProblem.getWikiLinkKey() != null);
        // Enable Resolve as soon as at least one target problem is resolvable,
        // so a whole ticked set (or the selected row) can be repaired at once.
        resolveProblemAction.setEnabled(hasResolvableTarget());
        // Always enable cleanup
        cleanupProblemAction.setEnabled(true);
    }

    /**
     * The problems a bulk action should act on: the ticked ones, or - when none
     * are ticked - the single selected row (preserving the old one-at-a-time
     * behaviour). Order follows the table.
     */
    private List<Problem> getTargetProblems() {
        List<Problem> checked = problemsTableModel.getCheckedProblems();
        if (!checked.isEmpty()) {
            return checked;
        }
        List<Problem> result = new ArrayList<>(1);
        if (selectedProblem != null) {
            result.add(selectedProblem);
        }
        return result;
    }

    /**
     * Whether at least one target problem can be repaired.
     */
    private boolean hasResolvableTarget() {
        for (Problem problem : getTargetProblems()) {
            if (problem instanceof ResolvableProblem) {
                return true;
            }
        }
        return false;
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    private class MyOpenProblemAction extends BaseAction {

        private MyOpenProblemAction(Controller controller) {
            super("action_open_problem", controller);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ProblemsTab.this.resolveProblem();
        }
    }

    private class MyResolveProblemAction extends BaseAction {
        MyResolveProblemAction(Controller controller) {
            super("action_resolve_problem", controller);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Repair every resolvable target problem, so the user can tick
            // several (or all) and fix them in one action. Collect first, then
            // resolve, since resolving removes rows and shifts indices.
            List<Problem> targets = getTargetProblems();
            boolean resolvedAny = false;
            for (Problem problem : targets) {
                if (problem instanceof ResolvableProblem) {
                    ResolvableProblem resolvableProblem = (ResolvableProblem) problem;
                    SwingUtilities.invokeLater(resolvableProblem
                        .resolution(getController()));
                    resolvableProblem.getFolder(getController())
                        .removeProblem(resolvableProblem);
                    resolvedAny = true;
                }
            }
            if (!resolvedAny) {
                logSevere("Tried to resolve with no resolvable problem targeted");
            }
        }
    }

    private class MyCleanupProblemAction extends BaseAction {
        MyCleanupProblemAction(Controller controller) {
            super("action_cleanup_problem", controller);
        }

        /**
         * Removes a problem, if it is resolvable.
         *
         * @param problem The problem
         */
        private void cleanup(Problem problem) {
            if (problem instanceof ResolvableProblem) {
                ResolvableProblem resolvableProblem = (ResolvableProblem) problem;
                resolvableProblem.ignore(getController());
            } else {
                logSevere("Tried to clean up a non-resolvable problem " + (problem == null ? null : problem.getClass().getName()));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<Problem> checked = problemsTableModel.getCheckedProblems();
            // Ticked problems: clean up each of them.
            if (!checked.isEmpty()) {
                for (Problem problem : checked) {
                    cleanup(problem);
                }
            }
            // Else a single selected row: clean that one up.
            else if (selectedProblem != null) {
                cleanup(selectedProblem);
            }
            // Else nothing selected: clean all problems up.
            else {
                while (problemsTableModel.getRowCount() > 0) {
                    cleanup(problemsTableModel.getProblemAt(0));
                }
            }
        }
    }

    /**
     * Class to detect table selection changes.
     */
    private class MySelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            enableOnSelection();
        }
    }

    private class TableMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    ProblemsTab.this.resolveProblem();
                }
            }
        }
    }

    public void resolveProblem() {
        Problem problem = problemsTableModel.getProblemAt(
            problemsTable.getSelectedRow());
        if (problem == null) {
            return;
        }
        String wikiArticleURL = Help.getWikiArticleURL(getController(),
            problem.getWikiLinkKey());
        if (StringUtils.isNotBlank(wikiArticleURL)) {
            BrowserLauncher.openURL(getController(), wikiArticleURL);
        }
    }
}
