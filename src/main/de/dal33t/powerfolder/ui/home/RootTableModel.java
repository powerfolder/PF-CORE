package de.dal33t.powerfolder.ui.home;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Maps the child nodes of the rootnode to a table model.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RootTableModel implements TableModel {
    private Set<TableModelListener> tableListeners = new HashSet<TableModelListener>();
    private Controller controller;
    private String[] columns = new String[]{
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.size")};

    public RootTableModel(Controller controller) {
        this.controller = controller;
        // UI Updating for the repository
        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        // UI Updater for connected nodes
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        // updater for up/downloads
        controller.getTransferManager().addListener(
            new MyTransferManagerListener());

        controller.addPropertyChangeListener(Controller.PROPERTY_NETWORKING_MODE,
            new MyControllerListener());
    }

    public Class getColumnClass(int columnIndex) {
        return Object.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        TreeNodeList rootNode = controller.getUIController()
            .getControlQuarter().getNavigationTreeModel().getRootNode();
        return rootNode.getChildCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        TreeNodeList rootNode = controller.getUIController()
            .getControlQuarter().getNavigationTreeModel().getRootNode();
        return rootNode.getChildAt(rowIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("editing not allowed");
    }

    public void addTableModelListener(TableModelListener l) {
        tableListeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        tableListeners.remove(l);
    }

    private void update() {
        Iterator it = tableListeners.iterator();
        while (it.hasNext()) {
            TableModelListener listener = (TableModelListener) it.next();
            listener.tableChanged(new TableModelEvent(this, 1, getRowCount()));
        }
    }

    /**
     * Listens for property changes on the controller
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyControllerListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            update();
        }
    }

    /**
     * Listener on folder repository
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
            update();
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
            update();
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            update();
        }

        public void folderCreated(FolderRepositoryEvent e) {
            update();
        }

        public void scansStarted(FolderRepositoryEvent e) {
        }

        public void scansFinished(FolderRepositoryEvent e) {
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyNodeManagerListener implements NodeManagerListener {
        public void friendAdded(NodeManagerEvent e) {
            update();
        }

        public void friendRemoved(NodeManagerEvent e) {
            update();
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            update();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            update();
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {
        public void downloadRequested(TransferManagerEvent event) {
            update();
        }
        
        public void downloadQueued(TransferManagerEvent event) {
            update();
        }

        public void downloadStarted(TransferManagerEvent event) {
            update();
        }

        public void downloadAborted(TransferManagerEvent event) {
            update();
        }

        public void downloadBroken(TransferManagerEvent event) {
            update();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            update();
        }
        
        public void completedDownloadRemoved(TransferManagerEvent event) {
            update();
        }
        
        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            update();
        }

        public void uploadRequested(TransferManagerEvent event) {
            update();
        }

        public void uploadStarted(TransferManagerEvent event) {
            update();
        }

        public void uploadAborted(TransferManagerEvent event) {
            update();
        }

        public void uploadBroken(TransferManagerEvent event) {
            update();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            update();
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }     
    }
}
