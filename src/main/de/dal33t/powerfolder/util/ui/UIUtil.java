/* $Id$
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Offers helper/utility method for UI related stuff.
 * <p>
 * TODO Move methods from <code>Util</code> here.^
 * 
 * @see de.dal33t.powerfolder.util.Util
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UIUtil {

    private static final Logger LOG = Logger.getLogger(UIUtil.class);

    /** Flag if awt is available */
    private static boolean AWTAvailable;

    // Initalize awt check
    static {
        // Okay lets check if we have an AWT system
        try {
            Color col = Color.RED;
            col.brighter();

            SimpleAttributeSet warn = new SimpleAttributeSet();
            StyleConstants.setForeground(warn, Color.RED);

            // Okay we have AWT
            AWTAvailable = true;
        } catch (Error e) {
            // ERROR ? Okay no AWT
            AWTAvailable = false;
        }
    }

    // UI Contstans
    /** The property name for the look and feel change on UIManager */
    public static final String UIMANAGER_LOOK_N_FEEL_PROPERTY = "lookAndFeel";

    /** The property name default dark control shadow color in UIManager */
    public static final String UIMANAGER_DARK_CONTROL_SHADOW_COLOR_PROPERTY = "controlDkShadow";

    private UIUtil() {
        // No instance allowed
    }

    /**
     * Answers if we have the AWT libs available
     * 
     * @return
     */
    public static boolean isAWTAvailable() {
        return AWTAvailable;
    }

    /**
     * Executes a task in the event dispatcher thread (if swing is available)
     * and waits until execution was finished.
     * <p>
     * If swing is not available the task gets directly executed.
     * 
     * @param task
     * @throws InterruptedException
     */
    public static void invokeAndWaitInEDT(Runnable task)
        throws InterruptedException
    {
        Reject.ifNull(task, "Task is null");
        if (!isAWTAvailable() || SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(
                    "Exception while executing in event dispatcher thread", e
                        .getCause());
            }
        }
    }

    /**
     * Executes a task in the event dispatcher thread (if swing is available).
     * The task just gets enqued in the event queue.
     * <p>
     * If swing is not available the task gets directly executed.
     * 
     * @param task
     */
    public static void invokeLaterInEDT(Runnable task) {
        Reject.ifNull(task, "Task is null");
        if (!isAWTAvailable() || SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * Whitestrips the table and returns it.
     * <p>
     * FIXME: Only works when the table is already i a scrollpane. better set
     * the background of the viewport from the scrollpane
     * 
     * @param table
     * @return
     */
    public static JTable whiteStripTable(JTable table) {
        table.getParent().setBackground(Color.WHITE);
        return table;
    }

    /**
     * Sets the preferred height of a component to zero (0).
     * <p>
     * Useful for <code>JScrollPanes</code>.
     * 
     * @param comp
     *            the component
     * @return the component
     */
    public static JComponent setZeroHeight(JComponent comp) {
        Dimension dims = comp.getPreferredSize();
        dims.height = 0;
        comp.setPreferredSize(dims);
        return comp;
    }

    /**
     * Removes the border from a component
     * 
     * @param comp
     * @return
     */
    public static JComponent removeBorder(JComponent comp) {
        comp.setBorder(Borders.EMPTY_BORDER);
        return comp;
    }

    /**
     * Sets the preferred width of a component to zero (0).
     * <p>
     * Useful for <code>JScrollPanes</code>.
     * 
     * @param comp
     *            the component
     * @return the component
     */
    public static JComponent setZeroWidth(JComponent comp) {
        Dimension dims = comp.getPreferredSize();
        dims.width = 0;
        comp.setPreferredSize(dims);
        return comp;
    }

    /**
     * Adds a task, which is executed, when the L&F changes
     * 
     * @param task
     */
    public static void addUIChangeTask(final Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task is null");
        }
        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (UIUtil.UIMANAGER_LOOK_N_FEEL_PROPERTY.equals(evt
                    .getPropertyName()))
                {
                    LOG.warn("UIManager changed l&f, executing task: " + task);
                    task.run();
                }
            }
        });
    }

    /** maps a column index of the view to the model column index */
    public static final int toModel(JTable table, int vColIndex) {
        if (vColIndex >= table.getColumnCount()) {
            return -1;
        }
        return table.getColumnModel().getColumn(vColIndex).getModelIndex();
    }

    /**
     * Returns the original user object, obj may have a userobject
     * (DefaultMutableTree, etc), it is returns else the object itself
     * 
     * @param obj
     * @return
     */
    public static Object getUserObject(Object obj) {
        Object userObject = obj;
        if (obj instanceof DefaultMutableTreeNode) {
            userObject = ((DefaultMutableTreeNode) obj).getUserObject();
        } else if (obj instanceof TreeNodeList) {
            userObject = ((TreeNodeList) obj).getUserObject();
        }

        // if userobject is null, return original
        return userObject != null ? userObject : obj;
    }

    /**
     * @param treeNode
     * @return the path to the treenode.
     */
    public static TreePath getPathTo(TreeNode treeNode) {
        Reject.ifNull(treeNode, "TreeNode is null");
        List alist = new ArrayList();
        TreeNode node = treeNode;
        do {
            alist.add(0, node);
            node = node.getParent();
        } while (node != null);
        Object[] pathArr = new Object[alist.size()];
        alist.toArray(pathArr);
        return new TreePath(pathArr);
    }

    /**
     * Sets a preferred minimum width in Dialog units (dlu) on a component
     * 
     * @param dlu
     *            the size in dlu
     * @param comp
     *            the component
     */
    public static void ensureMinimumWidth(int dlu, JComponent comp) {
        // Ensure minimum dimension
        Dimension dims = comp.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(dlu, comp);
        comp.setPreferredSize(dims);
    }
}
