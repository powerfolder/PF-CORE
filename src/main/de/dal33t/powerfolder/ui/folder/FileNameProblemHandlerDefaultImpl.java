package de.dal33t.powerfolder.ui.folder;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.AboutDialog2;
import de.dal33t.powerfolder.util.ui.BaseDialog;

public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{
    Map<FileInfo, List<String>> problems;
    Folder folder;
    
    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        log().debug(
            fileNameProblemEvent.getFolder() + " "
                + fileNameProblemEvent.getScanResult().getProblemFiles());
        problems = fileNameProblemEvent.getScanResult().getProblemFiles();
        folder = fileNameProblemEvent.getFolder();
        FileNameProblemDialog dialog = new FileNameProblemDialog(
            getController(), true);
        dialog.open();

    }

    public class FileNameProblemDialog extends BaseDialog {

        public FileNameProblemDialog(Controller controller, boolean modal,
            boolean border)
        {
            super(controller, modal, border);
        }

        public FileNameProblemDialog(Controller controller, boolean modal) {
            super(controller, modal);
        }

        @Override
        protected Component getButtonBar()
        {
            JButton cancelButton = createCancelButton(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    close();  
                }
            });
            
            JButton okButton = createOKButton(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    log().debug("okButton pressed");
                    setVisible(false);
                    close(); 
                }
            });

            ButtonBarBuilder builder = ButtonBarBuilder
                .createLeftToRightBuilder();
            builder.addRelatedGap();
            builder.addFixed(okButton);
            builder.addRelatedGap();
            builder.addFixed(cancelButton);
            return builder.getPanel();
        }

        @Override
        protected Component getContent()
        {   
            FormLayout layout = new FormLayout("pref","");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            int row = 1;
            for (FileInfo fileInfo : problems.keySet()){
                List<String> problemList = problems.get(fileInfo);
                JPanel panel = create(fileInfo, problemList);
                builder.appendRow(new RowSpec("pref:g"));
                builder.add(panel, cc.xy(1, row++));
            }
            
            return builder.getPanel();
        }

        @Override
        protected Icon getIcon()
        {
            return Icons.WARNING;
        }

        @Override
        public String getTitle()
        {
            return "File name problems detected";
        }

    }
    
    private JPanel create(FileInfo fileInfo,  List<String> problemList ) {
        FormLayout layout = new FormLayout(
            "pref, 4dlu, pref:g, 4dlu, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        // Build
        builder.add(new JLabel(fileInfo.getName()), cc.xy(1, 1));
        
        builder.add(new ProblemJList(problemList), cc.xy(3, 1));
        builder.add(new JButton("fix"), cc.xy(5, 1));
        builder.setBorder(new EmptyBorder(2,
            2, 2, 2));
        return builder.getPanel();
    }
    
    private class ProblemJList extends JList {
        public ProblemJList(List<String> list) {
            super(new MyListModel(list));
            setCellRenderer(new MyCellRenderer());
        }        
    }
    
    private class MyListModel extends AbstractListModel {
        List list;
        public MyListModel (List<String> list) {
            this.list = list;
        }
        public Object getElementAt(int index) {
            return list.get(index);
        }
        public int getSize() {
            return list.size();
        }        
    }
    private class MyCellRenderer  implements ListCellRenderer {

        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            return AboutDialog2.createHeaderTextPanel((String)value, 10);
        }
        
    }
}
