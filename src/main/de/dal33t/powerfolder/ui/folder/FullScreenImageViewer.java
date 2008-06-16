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
package de.dal33t.powerfolder.ui.folder;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.ImageFileInfo;
import de.dal33t.powerfolder.util.ImageSupport;

/**
 * If will use the fullscreen mode to display image for known image types else
 * it displayes an Operating System file icon. ScrollWheel is supported to
 * select the next/previous image. While image is being rendered (maybe slow
 * because we use smooth resize) a wait cursor is displayed.<BR>
 * TODO: hide on escape, commented out code does not work.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class FullScreenImageViewer extends Window implements MouseListener,
    MouseWheelListener
{
    private GraphicsDevice graphicsDevice;
    private BufferedImage bufferedImage;
    private FilesTab filesTab;
    private Controller controller;
    /** for scrollwheel, remembers the index */
    private int scrollWheelSelectionIndex = -1;

    public FullScreenImageViewer(Frame owner, FilesTab filesTab,
        Controller controller)
    {
        super(owner);
        this.filesTab = filesTab;
        this.controller = controller;
        GraphicsEnvironment env = GraphicsEnvironment
            .getLocalGraphicsEnvironment();
        graphicsDevice = env.getDefaultScreenDevice();
        this.addMouseListener(this);
        // listen to mousewheel events:
        this.addMouseWheelListener(this);
        /*
         * hmm this code does not work: ???? this.addKeyListener(new
         * KeyAdapter() { public void keyPressed(KeyEvent e) {
         * System.out.println("key: " + e.getKeyCode()); if (e.getKeyCode() ==
         * KeyEvent.VK_ESCAPE) { hide(); } } });
         */
    }

    /**
     * Set the image we chould display. Switches to fullscreen mode if
     * supported. Sets the cursor to wait cursor and calles the pain method
     * (indirecct per repaint()). Resets the scrollWheel index.
     */
    public void setImage(BufferedImage image) {
        // reset the scrollWheel index
        scrollWheelSelectionIndex = -1;
        this.bufferedImage = image;
        if (graphicsDevice.isFullScreenSupported()) {
            getOwner().setVisible(false);
            graphicsDevice.setFullScreenWindow(this);
            requestFocus();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            repaint();
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setSize(screenSize.width, screenSize.height);
            setVisible(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            repaint();
        }
    }

    /** takes care of paining the image, after paining set cursor to hand cursor. */
    public void paint(Graphics graphics) {
        // Get our painting dimensions
        int width = this.getWidth();
        int height = this.getHeight();
        if (width <= 0 || height <= 0) {
            graphics.dispose();
            return;
        }
        Image resizedImage = bufferedImage;
        // resize needed?
        if (width < bufferedImage.getWidth()
            || height < bufferedImage.getHeight())
        {
            Image newBufferedImage = ImageSupport.constrain(bufferedImage,
                width, height, Image.SCALE_SMOOTH);
            if (newBufferedImage != null) {// out of memory?
                bufferedImage.flush();
                resizedImage = newBufferedImage;
            }
        }

        if (resizedImage == null) { // out of memory?
            graphics.dispose();
            return;
        }
        // calculate where to draw, center image
        int imageWidth = resizedImage.getWidth(null);
        int imageHeight = resizedImage.getHeight(null);
        int paintX = width / 2 - imageWidth / 2;
        int paintY = height / 2 - imageHeight / 2;
        // Draw the image
        graphics.drawImage(resizedImage, paintX, paintY, imageWidth,
            imageHeight, this);
        graphics.dispose();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * restores the default cursor, disables fullscreen(if supported) and hides
     * this component.
     */
    public void hideMe() {
        Cursor def = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        setCursor(def);
        try {
            if (graphicsDevice.isFullScreenSupported()) {
                graphicsDevice.setFullScreenWindow(null);
                getOwner().setVisible(true);
            } else {
                setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** hide this component on mouseclick */
    public void mouseClicked(MouseEvent e) {
        hideMe();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /** display the next or previous image if mousewheel has moved. */
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        // may be negative!
        int clicksTurned = mouseWheelEvent.getWheelRotation();
        if (mouseWheelEvent.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
        {
            DirectoryTableModel directoryTableModel = (DirectoryTableModel) filesTab
                .getDirectoryTable().getModel();
            int indexOfSelection;
            if (scrollWheelSelectionIndex != -1) {
                // we need the relative index from our previous "turn"
                indexOfSelection = scrollWheelSelectionIndex;
            } else {
                // first time get the selected index from the table
                Object currentSelection = filesTab.getSelectionModel()
                    .getSelection();
                indexOfSelection = directoryTableModel
                    .getIndexOf(currentSelection);
            }

            if (indexOfSelection != -1) {
                int newSelectionIndex = indexOfSelection + clicksTurned;
                scrollWheelSelectionIndex = newSelectionIndex;
                // check bounds
                if (newSelectionIndex >= 0
                    && (directoryTableModel.getRowCount() - 1) >= newSelectionIndex)
                {
                    Object object = directoryTableModel.getValueAt(
                        newSelectionIndex, 0);
                    if (object instanceof ImageFileInfo) {

                        FileInfo fileInfo = (FileInfo) object;
                        Folder folder = controller.getFolderRepository()
                            .getFolder(fileInfo.getFolderInfo());
                        File file = folder.getDiskFile(fileInfo);
                        if (file.exists()) {
                            if (ImageSupport.isReadSupportedImage(file
                                .getAbsolutePath()))
                            {
                                bufferedImage = ImageSupport.getImage(file
                                    .getAbsoluteFile());
                                setCursor(Cursor
                                    .getPredefinedCursor(Cursor.WAIT_CURSOR));
                                repaint();
                            }
                        }
                    }
                }
            }
        }
    }
}