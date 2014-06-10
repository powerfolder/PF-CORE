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
 * $Id$
 */
package de.dal33t.powerfolder.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.ui.UIConstants;

/**
 * Offers helper/utility method for UI related stuff.
 * <p>
 *
 * @see Util
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class UIUtil {

    private static final Logger log = Logger.getLogger(UIUtil.class.getName());

    // The size of a medium sized font, e.g. the big subpoints on a wizard
    public static final int MED_FONT_SIZE = 15;

    // UI Contstans
    /** The property name for the look and feel change on UIManager */
    public static final String UIMANAGER_LOOK_N_FEEL_PROPERTY = "lookAndFeel";

    /** The property name default dark control shadow color in UIManager */
    public static final String UIMANAGER_DARK_CONTROL_SHADOW_COLOR_PROPERTY = "controlDkShadow";

    private UIUtil() {
        // No instance allowed
    }

    public static void setFontSize(JLabel label, int fontSize) {
        SimpleComponentFactory.setFont(label, fontSize, label.getFont()
            .getStyle());
    }

    public static void setFontStyle(JLabel label, int style) {
        SimpleComponentFactory.setFont(label, label.getFont().getSize(), style);
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
        if (!Util.isAwtAvailable() || SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(
                    "Exception while executing in event dispatcher thread",
                    e.getCause());
            }
        }
    }

    /**
     * Executes a task in the event dispatcher thread (if swing is available).
     * The task just gets queued in the event queue.
     * <p>
     * If swing is not available the task gets directly executed.
     *
     * @param task
     */
    public static void invokeLaterInEDT(Runnable task) {
        Reject.ifNull(task, "Task is null");
        if (Util.isAwtAvailable()) {
            SwingUtilities.invokeLater(task);
        } else {
            task.run();
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
     * @param event
     * @return the first Window in the action chaing. Useful for dialogs.
     */
    public static Window getParentWindow(ActionEvent event) {
        if (!(event.getSource() instanceof Component)) {
            return null;
        }
        Component comp = (Component) event.getSource();
        while (comp.getParent() != null) {
            if (comp instanceof Window) {
                // First frame
                return (Window) comp;
            }
            comp = comp.getParent();
        }
        if (comp instanceof Window) {
            // First frame
            return (Window) comp;
        }
        return null;
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
                    log.warning("UIManager changed l&f, executing task: "
                        + task);
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
     * @param obj
     * @return the original user object, obj may have a userobject
     *         (DefaultMutableTree, etc), it is returns else the object itself
     */
    public static Object getUserObject(Object obj) {
        Object userObject = obj;
        if (obj instanceof DefaultMutableTreeNode) {
            userObject = ((DefaultMutableTreeNode) obj).getUserObject();
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
        List<TreeNode> alist = new ArrayList<TreeNode>();
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

    /**
     * Apply opacity to a window. Done with reflection to ensure there is no
     * issue pre Java 1.6.0_10, although the Java version should already have
     * been checked ({@link Constants#OPACITY_SUPPORTED}).
     *
     * @param window
     * @param opacity
     */
    public static void applyTranslucency(Window window, Float opacity) {
        try {
            Class<?> clazz = Class.forName("com.sun.awt.AWTUtilities");
            Method m = clazz.getMethod("setWindowOpacity", Window.class,
                Float.TYPE);
            m.invoke(clazz, window, opacity);
        } catch (NoSuchMethodException e) {
            log.warning(e.getMessage());
        } catch (InvocationTargetException e) {
            log.warning(e.getMessage());
        } catch (ClassNotFoundException e) {
            log.warning(e.getMessage());
        } catch (IllegalAccessException e) {
            log.warning(e.getMessage());
        }
    }

    /**
     * This forces a frame to be on screen, with all of its edges visible
     *
     * @param frame
     */
    public static void putOnScreen(JFrame frame) {

        if (frame.getWidth() <= 0 || frame.getHeight() <= 0) {
            // Something has gone very wrong with the size.
            // Apply default size.
            frame.pack();
            int targetWidth = Math.max(frame.getWidth(),
                    UIConstants.DEFAULT_FRAME_WIDTH);
            int targetHeight = Math.max(frame.getHeight(),
                    UIConstants.DEFAULT_FRAME_HEIGHT);
            frame.setSize(targetWidth, targetHeight);
        }

        // Make sure the location is on the screen.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (frame.getX() < 0) {
            frame.setLocation(0, frame.getY());
        }
        if (frame.getY() < 0) {
            frame.setLocation(frame.getX(), 0);
        }
        if (frame.getX() > screenSize.width) {
            frame
                .setLocation(screenSize.width - frame.getWidth(), frame.getY());
        }
        if (frame.getY() > screenSize.height) {
            frame.setLocation(0, screenSize.height - frame.getHeight());
        }
    }

    /**
     * This forces a frame to be on screen, with all of its edges visible
     *
     * @param dialog
     */
    public static void putOnScreen(JDialog dialog) {

        // Make sure the location is on the screen.
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (dialog.getX() < 0) {
            dialog.setLocation(0, dialog.getY());
        }
        if (dialog.getY() < 0) {
            dialog.setLocation(dialog.getX(), 0);
        }
        if (dialog.getX() > screenSize.width) {
            dialog
                .setLocation(screenSize.width - dialog.getWidth(), dialog.getY());
        }
        if (dialog.getY() > screenSize.height) {
            dialog.setLocation(0, screenSize.height - dialog.getHeight());
        }
    }

    public static void setMacDockImage(Image img) {
        if (!OSUtil.isMacOS()) {
            return;
        }
        try {
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Method getApplication = appClass.getMethod("getApplication");
            Object application = getApplication.invoke(null);
            Method setDockIconImage = appClass.getMethod("setDockIconImage",
                Image.class);
            setDockIconImage.invoke(application, img);
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to dock image. " + e, e);
        }

        // com.apple.eawt.Application
        // try {
        // Application app = Application.getApplication();
        // app.setDockIconImage(Icons.getImageById(Icons.SYSTRAY_DEFAULT));
        // } catch (Exception e) {
        // logSevere(e);
        // }

    }

    /**
     * #2440
     *
     * @return the screen width of all screens summarized.
     */
    public static int getScreenWidthAllMonitors() {
        try {
            int width = 0;
            GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            for (GraphicsDevice curGs : gs) {
                DisplayMode mode = curGs.getDisplayMode();
                width += mode.getWidth();
            }
            return width;
        } catch (Exception e) {
            log.warning("Unable to get screen configuration. " + e);
            return Toolkit.getDefaultToolkit().getScreenSize().width;
        }
    }
}
