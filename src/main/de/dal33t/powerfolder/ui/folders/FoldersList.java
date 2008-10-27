package de.dal33t.powerfolder.ui.folders;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;

import javax.swing.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;

public class FoldersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel folderListPanel;
    private final List<ExpandableFolderView> views;

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    public FoldersList(Controller controller) {
        super(controller);
        views = new ArrayList<ExpandableFolderView>();
    }

    private void buildUI() {

        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(folderListPanel, cc.xy(1, 1));
        uiComponent = builder.getPanel();
    }

    private void initComponents() {
        folderListPanel = new JPanel();
        folderListPanel.setLayout(new BoxLayout(folderListPanel, BoxLayout.PAGE_AXIS));
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            ExpandableFolderView folderView = new ExpandableFolderView(getController(), folder);
            folderListPanel.add(folderView.getUIComponent());
        }
        registerListeners();
    }

    private void registerListeners() {
        getController().getFolderRepository().addFolderRepositoryListener(new MyFolderRepositoryListener());
    }

    private class MyFolderRepositoryListener implements FolderRepositoryListener {

        public void folderRemoved(FolderRepositoryEvent e) {
            String folderName = e.getFolder().getName();
            Component[] components = folderListPanel.getComponents();
            for (ExpandableFolderView view : views) {
                if (view.getFolderName().equals(folderName)) {
                    for (Component component : components) {
                        if (component.equals(view.getUIComponent())) {
                            folderListPanel.remove(component);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        public void folderCreated(FolderRepositoryEvent e) {
            ExpandableFolderView view = new ExpandableFolderView(getController(), e.getFolder());
            folderListPanel.add(view.getUIComponent());
            views.add(view);
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

}
