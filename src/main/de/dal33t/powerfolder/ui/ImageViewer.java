package de.dal33t.powerfolder.ui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JComponent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.folder.FilesTab;
import de.dal33t.powerfolder.ui.folder.FullScreenImageViewer;
import de.dal33t.powerfolder.util.ImageSupport;

/**
 * Displayes an Image for known Image File types else it will display a System
 * File icon. Click on the image will open a FullScreenImageViewer.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.12 $
 */
public class ImageViewer extends JComponent implements MouseListener {
    private BufferedImage bufferedImage;
    private Controller controller;
    private FullScreenImageViewer fullScreenImageViewer;
    private File file;
    // needed in the fullscreen ImageViewer to get the next image if scrollwheel
    // is used.
    private FilesTab filesTab;

    public ImageViewer(Controller controller, FilesTab filesTab) {
        super();
        this.filesTab = filesTab;
        this.controller = controller;
        this.addMouseListener(this);
    }

    public void setImageFile(File file, FileInfo fileInfo) {
        if (file != null && this.file != null && file.equals(this.file)) {
            return;// same file don't need to do anything
        }
        this.file = file;
        if (bufferedImage != null) {
            bufferedImage.flush();
        }
        if (file == null || !file.exists()
            || !ImageSupport.isReadSupportedImage(file.getAbsolutePath()))
        {
            bufferedImage = null;
            ImageSupport.clearCache();
            if (file != null) { // try to display the icon files that are not
                // images or that not exists local.
                bufferedImage = Icons.toBufferedImage(Icons
                    .getImageFromIcon(Icons.getIconFor(fileInfo, controller)));
            }
        } else {
            bufferedImage = ImageSupport.getImage(file.getAbsoluteFile());
        }
        repaint();
    }

    public JComponent getUIComponent() {
        return this;
    }

    public void paint(Graphics graphics) {
        // Get our painting dimensions
        int width = this.getWidth();
        int height = this.getHeight();

        if (bufferedImage == null) {
            graphics.setColor(this.getBackground());
            graphics.fillRect(0, 0, width, height);
        } else {
            if (width <= 0 || height <= 0) {
                graphics.dispose();
                return;
            }
            Image resizedImage = bufferedImage;
            if (width < bufferedImage.getWidth()
                || height < bufferedImage.getHeight())
            {
                resizedImage = ImageSupport.constrain(bufferedImage, width,
                    height, Image.SCALE_FAST);
            }
            if (resizedImage == null) { // out of memory?
                graphics.dispose();
                return;
            }
            int imageWidth = resizedImage.getWidth(null);
            int imageHeight = resizedImage.getHeight(null);
            int paintX = width / 2 - imageWidth / 2;
            int paintY = height / 2 - imageHeight / 2;
            // Draw the image
            graphics.drawImage(resizedImage, paintX, paintY, imageWidth,
                imageHeight, this);
        }
        graphics.dispose();
    }

    public void mouseClicked(MouseEvent e) {
        if (bufferedImage != null) {
            if (fullScreenImageViewer == null) {
                fullScreenImageViewer = new FullScreenImageViewer(controller
                    .getUIController().getMainFrame().getUIComponent(),
                    filesTab, controller);
            }
            fullScreenImageViewer.setImage(bufferedImage);
        }
    }

    public void mouseEntered(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
