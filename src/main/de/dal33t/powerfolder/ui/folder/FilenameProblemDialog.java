package de.dal33t.powerfolder.ui.folder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.TextLinesPanelBuilder;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays a dialog if filename problems are found. This mainly happnes on
 * linux, since thos file systems allow almost all characters
 */
public class FilenameProblemDialog extends PFUIComponent {
    private String[] columns = new String[]{
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.description"),
        Translation.getTranslation("filenameproblemhandler.solution")};

    private static final int FILENAME_COLUMN = 0;
    private static final int PROBLEM_COLUMN = 1;
    private static final int SOLUTION_COLUMN = 2;
    private JDialog dialog;
    private JPanel panel;
    private JScrollPane tableScroller;
    private JPanel toolbar;

    private enum Solution {
        NOTHING, RENAME, ADD_TO_IGNORE
    }

    private Map<FileInfo, Solution> solutionsMap;
    /**
     * a list of FileInfos in a fixed order (keysets are of a not determent
     * order)
     */
    private List<FileInfo> problemList;
    private ScanResult scanResult;

    private JTable table;

    public FilenameProblemDialog(Controller controller, ScanResult scanResult) {
        super(controller);
        this.scanResult = scanResult;
        problemList = new ArrayList<FileInfo>(scanResult.getProblemFiles()
            .keySet());
        solutionsMap = new HashMap<FileInfo, Solution>();
        // add default solution for each file "do nothing"
        for (FileInfo fileInfo : problemList) {
            solutionsMap.put(fileInfo, Solution.NOTHING);
        }

    }

    public void open() {
        initComponents();
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent(),
            "title", true); // modal
        dialog.getContentPane().add(getUIComponent());
        dialog.pack();
        dialog.setVisible(true);
    }

    /** returns this ui component, creates it if not available */
    private Component getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(tableScroller, cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.add(toolbar, cc.xy(1, 3));
            panel = builder.getPanel();
        }
        return panel;
    }

    private JPanel createToolbar() {
        JButton cancel = new JButton(Translation
            .getTranslation("general.cancel"));
        cancel.setMnemonic(Translation.getTranslation("general.cancel.key")
            .trim().charAt(0));
        JButton ok = new JButton(Translation.getTranslation("general.ok"));
        ok.setMnemonic(Translation.getTranslation("general.ok.key").trim()
            .charAt(0));

        JPanel buttons = ButtonBarFactory.buildRightAlignedBar(cancel, ok);
        buttons.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttons.setOpaque(false);

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }
        });

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                log().debug("okButton pressed");
                doSolutions();
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }
        });
        return buttons;
    }

    private void initComponents() {
        table = new JTable(new ProblemTableModel());
        ProblemTableCellRenderer problemTableCellRenderer = new ProblemTableCellRenderer();
        table.setDefaultRenderer(Object.class, problemTableCellRenderer);
        table.setDefaultEditor(Object.class, problemTableCellRenderer);
        table.getTableHeader().setReorderingAllowed(false);
        tableScroller = new JScrollPane(table);

        toolbar = createToolbar();
        setColumnSizes(table);
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tableScroller);

    }

    private void setColumnSizes(JTable table) {
        table.setRowHeight(105);
        // otherwise the table header may not be visible:
        table.getTableHeader().setPreferredSize(new Dimension(600, 20));
        TableColumn column = table.getColumn(table.getColumnName(0));

        column.setPreferredWidth(150);

        column = table.getColumn(table.getColumnName(1));
        column.setPreferredWidth(500);
        column = table.getColumn(table.getColumnName(2));
        column.setPreferredWidth(150);

    }

    private void doSolutions() {
        for (FileInfo fileInfo : solutionsMap.keySet()) {
            Solution solution = solutionsMap.get(fileInfo);
            switch (solution) {
                case NOTHING : {
                    break;
                }
                case RENAME : {
                    doRename(fileInfo);
                    break;
                }
                case ADD_TO_IGNORE : {
                    Folder folder = getController().getFolderRepository()
                        .getFolder(fileInfo.getFolderInfo());
                    folder.getBlacklist().add(fileInfo);
                    break;
                }
                default :
                    throw new IllegalStateException("illegal solution");
            }
        }
    }

    private void doRename(FileInfo fileInfo) {
        // a filename may have more problems
        // try solve them all if not solved after first solution
        List<FilenameProblem> problems = scanResult.getProblemFiles().get(
            fileInfo);
        FilenameProblem problem = problems.get(0);
        FileInfo fileInfoSolved = problem.solve(getController());
        if (fileInfoSolved != null) {
            int count = 1;
            while (fileInfoSolved != null
                && FilenameProblem
                    .hasProblems(fileInfoSolved.getFilenameOnly()))
            {
                if (problems.size() >= count++) {
                    problem = problems.get(count);
                    // make sure to use the correct new filename
                    problem.setFileInfo(fileInfoSolved);
                    fileInfoSolved = problem.solve(getController());
                } else {
                    break;
                }
            }
        }

        if (fileInfoSolved == null) {
            log().warn(
                "something went wrong with solving the filename problems for:"
                    + fileInfo);
        } else {
            scanResult.getNewFiles().remove(fileInfo);
            scanResult.getNewFiles().add(fileInfoSolved);
            Folder folder = getController().getFolderRepository().getFolder(
                fileInfo.getFolderInfo());
            if (folder.isKnown(fileInfo)) {
                File oldDiskFile = folder.getDiskFile(fileInfo);
                if (!oldDiskFile.exists()) {
                    scanResult.getDeletedFiles().add(fileInfo);
                }
                scanResult.getMovedFiles().put(fileInfo, fileInfoSolved);
            }
        }
    }

    private class ProblemJList extends JList {
        public ProblemJList(List<FilenameProblem> list) {
            super(new MyListModel(list));
            setCellRenderer(new MyCellRenderer());
        }
    }

    private class MyListModel extends AbstractListModel {
        List<FilenameProblem> list;

        public MyListModel(List<FilenameProblem> list) {
            this.list = list;
        }

        public Object getElementAt(int index) {
            return list.get(index);
        }

        public int getSize() {
            return list.size();
        }
    }

    private class MyCellRenderer implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {
            JPanel panel = TextLinesPanelBuilder.createTextPanel(
                ((FilenameProblem) value).describeProblem(), 10);
            return panel;
        }
    }

    private class ProblemTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return columns.length;
        }

        public int getRowCount() {
            return scanResult.getProblemFiles().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return scanResult.getProblemFiles();
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return columns[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            // use a editor because the else the events are not passed to the
            // scrollpane or button
            if (columnIndex == PROBLEM_COLUMN || columnIndex == SOLUTION_COLUMN)
            {
                return true;
            }
            return false;
        }
    }

    private class ProblemTableCellRenderer extends AbstractCellEditor implements
        TableCellRenderer, TableCellEditor
    {

        private Map<FileInfo, JPanel> solutionsPanelCache;

        public ProblemTableCellRenderer() {
            solutionsPanelCache = new HashMap<FileInfo, JPanel>();
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            FileInfo fileInfo = problemList.get(row);
            switch (column) {
                case FILENAME_COLUMN : {
                    JLabel label = new JLabel(fileInfo.getName());
                    label.setToolTipText(fileInfo.getName());
                    return label;
                }
                case PROBLEM_COLUMN : {
                    return getProblemComponent(fileInfo);

                }
                case SOLUTION_COLUMN : {
                    return getSolutionComponent(fileInfo);
                }
            }
            return null;
        }

        private Component getSolutionComponent(final FileInfo fileInfo) {
            if (solutionsPanelCache.containsKey(fileInfo)) {
                return solutionsPanelCache.get(fileInfo);
            }

            FormLayout layout = new FormLayout("pref", "pref, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            JRadioButton nothingRadioButton = new JRadioButton("nothing");
            JRadioButton renameRadioButton = new JRadioButton("rename to ...");
            JRadioButton addToIgnoreRadioButton = new JRadioButton(
                "add to ignore");
            nothingRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    log().debug("addToIgnoreRadioButton action");
                    solutionsMap.put(fileInfo, Solution.NOTHING);
                }
            });

            renameRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    log().debug("renameRadioButton action");
                    solutionsMap.put(fileInfo, Solution.RENAME);
                }
            });

            addToIgnoreRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    log().debug("addToIgnoreRadioButton action");
                    solutionsMap.put(fileInfo, Solution.ADD_TO_IGNORE);
                }
            });

            nothingRadioButton.setSelected(true);

            nothingRadioButton.setBackground(Color.WHITE);
            renameRadioButton.setBackground(Color.WHITE);
            addToIgnoreRadioButton.setBackground(Color.WHITE);

            ButtonGroup group = new ButtonGroup();
            group.add(nothingRadioButton);
            group.add(renameRadioButton);
            group.add(addToIgnoreRadioButton);

            builder.add(nothingRadioButton, cc.xy(1, 1));
            builder.add(renameRadioButton, cc.xy(1, 2));
            builder.add(addToIgnoreRadioButton, cc.xy(1, 3));
            JPanel panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
            solutionsPanelCache.put(fileInfo, panel);
            return panel;
        }

        private Component getProblemComponent(FileInfo fileInfo) {
            JList jList = new ProblemJList(scanResult.getProblemFiles().get(
                fileInfo));
            List<FilenameProblem> problemDesctiptions = scanResult
                .getProblemFiles().get(fileInfo);
            String tooltip = "";
            String line = "";
            for (FilenameProblem problem : problemDesctiptions) {
                tooltip += line + problem.describeProblem();
                line = "<hr>";
            }
            int index = tooltip.indexOf("\n");
            while (index != -1) {
                String before = tooltip.substring(0, index);
                String after = tooltip.substring(index + 1, tooltip.length());
                tooltip = before + "<br>" + after;
                index = tooltip.indexOf("\n");
            }
            tooltip = "<html>" + tooltip + "</html>";
            jList.setToolTipText(tooltip);
            jList.setSize(jList.getPreferredSize());
            JScrollPane pane = new JScrollPane(jList);
            UIUtil.removeBorder(pane);
            pane.setToolTipText(tooltip);
            pane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            return pane;
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column)
        {
            if (!(column == PROBLEM_COLUMN || column == SOLUTION_COLUMN)) {
                throw new IllegalStateException(
                    "only problem and solution column use an editor");
            }
            switch (column) {
                case PROBLEM_COLUMN : {
                    FileInfo fileInfo = problemList.get(row);
                    return getProblemComponent(fileInfo);
                }
                case SOLUTION_COLUMN : {
                    FileInfo fileInfo = problemList.get(row);
                    return getSolutionComponent(fileInfo);
                }
            }
            return null;
        }
    }
}
