package de.dal33t.powerfolder.ui.folder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Toolbar;
import de.dal33t.powerfolder.ui.widget.AntialiasedLabel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays a dialog if filename problems are found. This mainly happens on
 * linux, since those file systems allow almost all characters
 */
public class FilenameProblemDialog extends PFUIComponent {
    private String[] columns = new String[]{
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.description"),
        Translation.getTranslation("filenameproblem.dialog.solution")};

    private int option = -1;

    public final static int OK = 1;

    public final static int CANCEL = 2;

    private static final int FILENAME_COLUMN = 0;

    private static final int PROBLEM_COLUMN = 1;

    private static final int SOLUTION_COLUMN = 2;

    private final int rowHeigth = 65;

    private JDialog dialog;

    private JPanel panel;

    private JScrollPane tableScroller;

    private JPanel toolbar;

    private JCheckBox neverAskAgainJCheckBox;

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

    /** either OK or CANCEL */
    public int getOption() {
        return option;
    }

    public void open() {
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent(),
            Translation.getTranslation("filenameproblem.dialog.title"), true); // modal
        dialog.setContentPane(getUIComponent());
        Component parent = dialog.getOwner();

        // Use 80% of the screen height at maximum!
        int maxHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.8);
        dialog.setMaximumSize(new Dimension(dialog.getMaximumSize().width, Math
            .min(dialog.getPreferredSize().height, maxHeight)));
        dialog.setPreferredSize(new Dimension(dialog.getPreferredSize().width,
            Math.min(dialog.getPreferredSize().height, maxHeight)));
        
        dialog.pack();
        int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
        int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setVisible(true);
    }

    /** returns this ui component, creates it if not available */
    private JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout(
                "4dlu, fill:pref:grow, pref, 4dlu",
                "7dlu, pref, 7dlu, fill:default, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(SimpleComponentFactory.createBigTextLabel(Translation
                .getTranslation("filenameproblem.dialog.description")), cc.xy(
                2, 2));
            builder.add(tableScroller, cc.xyw(2, 4, 2));
            builder.add(neverAskAgainJCheckBox, cc.xy(2, 5));
            builder.add(toolbar, cc.xy(3, 5));
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

        JPanel buttons = ButtonBarFactory.buildCenteredBar(ok, cancel);
        buttons.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
        buttons.setOpaque(false);

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                option = CANCEL;
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }
        });

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSolutions();
                option = OK;
                dialog.setVisible(false);
                dialog.dispose();
                dialog = null;
            }
        });
        return buttons;
    }

    public boolean askAgain() {
        return !neverAskAgainJCheckBox.isSelected();
    }

    private void initComponents() {
        neverAskAgainJCheckBox = new JCheckBox(Translation
            .getTranslation("filenameproblem.dialog.never_ask_this_again"));
        table = new JTable(new ProblemTableModel());
        ProblemTableCellRenderer problemTableCellRenderer = new ProblemTableCellRenderer();
        table.setDefaultRenderer(Object.class, problemTableCellRenderer);
        table.setDefaultEditor(Object.class, problemTableCellRenderer);
        table.getTableHeader().setReorderingAllowed(false);

        setColumnSizes(table);
        tableScroller = new JScrollPane(table);
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tableScroller);
        UIUtil.removeBorder(tableScroller);
        toolbar = createToolbar();

        UIUtil.whiteStripTable(table);
        tableScroller.setPreferredSize(new Dimension(500, rowHeigth
            * problemList.size() + table.getTableHeader().getHeight()));
    }

    private void setColumnSizes(JTable table) {
        table.setRowHeight(rowHeigth);
        table.setPreferredSize(new Dimension(600, rowHeigth
            * problemList.size() + table.getTableHeader().getHeight()));
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
                    folder.getBlacklist().addExplicit(fileInfo);
                    break;
                }
                default :
                    throw new IllegalStateException("illegal solution");
            }
        }
    }

    /**
     * a filename may have more problems this method tries to solve them all if
     * not solved after first solution
     */
    private void doRename(FileInfo fileInfo) {
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
            // update the scanresult
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

    /** maps the lists of files with problems to a table model */
    private class ProblemTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return columns.length;
        }

        public int getRowCount() {
            return problemList.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return problemList.get(rowIndex);
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return columns[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            // use an editor because the else the events are not passed to the
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
            FileInfo fileInfo = (FileInfo) value;
            switch (column) {
                case FILENAME_COLUMN : {
                    JLabel label = new JLabel(fileInfo.getName());
                    label.setBorder(Borders
                        .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
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

            JRadioButton nothingRadioButton = new JRadioButton(Translation
                .getTranslation("filenameproblem.dialog.do_nothing"));
            JRadioButton renameRadioButton = new JRadioButton(Translation
                .getTranslation("filenameproblem.dialog.automatic_rename"));
            renameRadioButton
                .setToolTipText(Translation
                    .getTranslation("filenameproblem.dialog.automatic_rename.explained"));
            JRadioButton addToIgnoreRadioButton = new JRadioButton(Translation
                .getTranslation("filenameproblem.dialog.add_to_ignore"));
            addToIgnoreRadioButton
                .setToolTipText(Translation
                    .getTranslation("filenameproblem.dialog.add_to_ignore.explained"));
            nothingRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    solutionsMap.put(fileInfo, Solution.NOTHING);
                }
            });

            renameRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    solutionsMap.put(fileInfo, Solution.RENAME);
                }
            });

            addToIgnoreRadioButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
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

            FormLayout layout = new FormLayout("pref", "pref, pref, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(nothingRadioButton, cc.xy(1, 1));
            builder.add(renameRadioButton, cc.xy(1, 2));
            builder.add(addToIgnoreRadioButton, cc.xy(1, 3));

            JPanel panel = builder.getPanel();
            panel
                .setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            panel.setBackground(Color.WHITE);
            solutionsPanelCache.put(fileInfo, panel);
            return panel;
        }

        private Component getProblemComponent(FileInfo fileInfo) {
            // display only first problem
            FilenameProblem problem = scanResult.getProblemFiles()
                .get(fileInfo).get(0);
            JLabel label = SimpleComponentFactory.createLabel(problem
                .shortDescription());
            label.setBackground(Color.WHITE);
            VisualLinkLabel detailsLabel = new VisualLinkLabel("details");
            FormLayout layout = new FormLayout("pref:grow", "pref, pref");
            CellConstraints cc = new CellConstraints();
            PanelBuilder builder = new PanelBuilder(layout);
            builder.add(label, cc.xy(1, 1));
            builder.add(detailsLabel, cc.xy(1, 2));
            JPanel panel = builder.getPanel();
            panel.setToolTipText(getTooltip(fileInfo));
            panel
                .setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            panel.setBackground(Color.WHITE);
            return panel;
        }

        private String getTooltip(FileInfo fileInfo) {
            List<FilenameProblem> problemDesctiptions = scanResult
                .getProblemFiles().get(fileInfo);
            String tooltip = "";
            String line = "";
            for (FilenameProblem aProblem : problemDesctiptions) {
                tooltip += line + aProblem.describeProblem();
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
            return tooltip;
        }

        private class VisualLinkLabel extends AntialiasedLabel {
            public VisualLinkLabel(String text) {
                super("<html><font color=\"#00000\"><a href=\"\">" + text
                    + "</a></font></html>");
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
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