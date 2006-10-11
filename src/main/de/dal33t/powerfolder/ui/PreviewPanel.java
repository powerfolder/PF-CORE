package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.folder.FilesTab;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * A preview of the file, now displayes an (small) image for known image types
 * and the Operating System icons for other files.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.11 $
 */
public class PreviewPanel extends PFUIComponent implements
    SelectionChangeListener
{
    private JPanel panel;
    private ImageViewer imageViewer;
    private FileInfo fileInfo;
    private boolean initDone;
    private FilesTab filesTab;

    public PreviewPanel(Controller controller, SelectionModel selectionModel,
        FilesTab filesTab)
    {
        super(controller);
        this.filesTab = filesTab;
        selectionModel.addSelectionChangeListener(this);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    /** can not do this on initComponents() because parent is still null then */
    private void initShowHideListener() {
        if (!initDone) {
            initDone = true;
            // listen to parent because the parent of this proview panel will be
            // show and hidden.
            panel.getParent().addComponentListener(new ComponentAdapter() {
                public void componentHidden(ComponentEvent e) {
                    // when component not visible set image viewer to null, to
                    // free up allocated image memory
                    imageViewer.setImageFile(null, null);
                }

                public void componentShown(ComponentEvent e) {
                    if (fileInfo != null) {
                        setFile(fileInfo);
                    }
                }
            });
        }
    }

    private void setFile(FileInfo fileInfo) {
        initShowHideListener();
        this.fileInfo = fileInfo;
        if (panel.isShowing() && fileInfo != null)
        // now display the icon if it's not an image
        // && fileInfo instanceof ImageFileInfo)
        {
            Folder folder = getController().getFolderRepository().getFolder(
                fileInfo.getFolderInfo());
            File file = folder.getDiskFile(fileInfo);
            // if (file.exists()) {
            imageViewer.setImageFile(file, fileInfo);
            return;
            // }
        }
        imageViewer.setImageFile(null, null);
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        imageViewer = new ImageViewer(getController(), filesTab);
        BorderLayout layout = new BorderLayout();
        panel = new JPanel(layout);
        panel.add(imageViewer.getUIComponent(), BorderLayout.CENTER);
        panel.setBorder(Borders.createEmptyBorder("2dlu, 2dlu, 2dlu, 2dlu"));
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof FileInfo) {
            setFile((FileInfo) selection);
        }
    }
}
