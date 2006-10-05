package de.dal33t.powerfolder.ui.folder;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ui.BaseDialog;

public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{

    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        log().debug(
            fileNameProblemEvent.getFolder() + " "
                + fileNameProblemEvent.getScanResult().getProblemFiles());
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
            return new JToolBar();
        }

        @Override
        protected Component getContent()
        {
            return new JLabel("tests");
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
}
