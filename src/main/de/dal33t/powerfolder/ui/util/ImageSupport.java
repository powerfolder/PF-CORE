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
* $Id: ImageSupport.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * Class with some convenience methods for image handeling.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.13 $
 */
public class ImageSupport {

    private static final Logger log = Logger.getLogger(ImageSupport.class.getName());

    // Use a set to filter duplicates
    private static final Set<String> supportedReadFileTypes = new HashSet<String>();
    private static final Set<String> supportedWriteFileTypes = new HashSet<String>();

    private static BufferedImage lastImage;
    private static int lastWidth;
    private static int lastHeight;
    private static Image lastResizedImage;
    private static int lastScalingType;

    static {
        Iterator<ImageReaderSpi> it = IIORegistry.getDefaultInstance()
            .getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            supportedReadFileTypes.addAll(Arrays.asList(spi.getFileSuffixes()));
        }
        supportedReadFileTypes.remove(""); // Seems to get added for some
        // reason...

        Iterator<ImageWriterSpi> writers = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, true);
        while (writers.hasNext()) {
            ImageWriterSpi spi = writers.next();
            supportedWriteFileTypes.addAll(Arrays.asList(spi.getFileSuffixes()));
        }
    }

    /**
     * is there a image reader available for a file with this extension?
     *
     * @param filename
     *            the file to determen if there is a imageReader for.
     * @return true if there is a ImageReader for this type of file.
     */
    public static boolean isReadSupportedImage(String filename) {
        int index = filename.lastIndexOf('.');
        if (index > 0) {
            String fileSuffix = filename
                .substring(index + 1, filename.length()).toLowerCase();
            if (supportedReadFileTypes.contains(fileSuffix)) {
                return true;
            }
        }
        return false;
    }

    /** @param extension the extension to determen if there is an image writer for */
    public static boolean isWriteSupportedImage(String extension) {
        return supportedWriteFileTypes.contains(extension);
    }

    /**
     * Get the resolution for an image, note: isReadSupportedImage must be true
     * for this file and file must exists.
     *
     * @param file
     * @return A Dimension holding the width and height of this image
     */
    public static Dimension getResolution(Path file) {
        if (!isReadSupportedImage(file.getFileName().toString())) {
            throw new IllegalStateException("unsuported image format: " + file);
        }
        if (Files.notExists(file)) {
            throw new IllegalStateException("File must exists: " + file);
        }
        try {
            int index = file.getFileName().toString().lastIndexOf('.');
            if (index > 0) {
                String fileSuffix = file.getFileName().toString().substring(index + 1,
                    file.getFileName().toString().length());
                ImageReader reader = getReader(fileSuffix);
                ImageInputStream iis = ImageIO.createImageInputStream(file);
                reader.setInput(iis, true);
                int imageIndex = 0;
                int width = reader.getWidth(imageIndex);
                int height = reader.getHeight(imageIndex);
                iis.close();
                reader.dispose();
                return new Dimension(width, height);
            }
        } catch (Exception e) {
            log.log(Level.FINER, "Unable to read image: " + file.toAbsolutePath(), e);
        }
        return null;
    }

    public static BufferedImage getImage(Path file) {
        if (!isReadSupportedImage(file.getFileName().toString())) {
            throw new IllegalStateException("unsuported image format: " + file);
        }
        if (Files.exists(file)) {
            throw new IllegalStateException("File must exists: " + file);
        }
        try {
            int index = file.getFileName().toString().lastIndexOf('.');
            if (index > 0) {
                String fileSuffix = file
                    .getFileName()
                    .toString()
                    .substring(index + 1,
                        file.getFileName().toString().length());
                ImageReader reader = getReader(fileSuffix);
                ImageInputStream iis = ImageIO.createImageInputStream(file);
                reader.setInput(iis, true);
                int imageIndex = 0;
                BufferedImage image = reader.read(imageIndex, null);
                iis.close();
                reader.dispose();
                return image;
            }
        } catch (Exception e) {
            log.log(Level.FINER, "Unable to read image: " + file.toAbsolutePath(), e);
        }
        return null;
    }

    private static ImageReader getReader(String fileSuffix) {
        Iterator<ImageReader> readers = ImageIO
            .getImageReadersByFormatName(fileSuffix);
        return readers.next();
    }


    public static void clearCache() {
        if (lastImage != null) {
            lastImage.flush();
            lastImage = null;
        }
        if (lastResizedImage != null) {
            lastResizedImage.flush();
            lastResizedImage = null;
        }
        lastWidth = -1;
        lastHeight = -1;
        lastScalingType = -1;
    }

    /**
     * Constrains an Image (or BufferedImage) to a specific width and heigth; if
     * the source image is smaller than width X height then its size is
     * increased to fit
     */
    public static Image constrain(BufferedImage src, int width, int height,
        int scalingType)
    {
        if (src == lastImage && width == lastWidth && height == lastHeight
            && scalingType == lastScalingType)
        {
            return lastResizedImage;
        }
        clearCache();
        lastWidth = width;
        lastHeight = height;
        lastImage = src;
        lastScalingType = scalingType;
        try {
            // Get the source Image dimensions
            int srcWidth = src.getWidth(null);
            int srcHeight = src.getHeight(null);

            // Compute the width to height ratios of both the source and the
            // constrained window
            double srcRatio = (double) srcWidth / (double) srcHeight;
            double windowRatio = (double) width / (double) height;

            // These variables will hold the destination dimensions
            int destWidth = width;
            int destHeight = height;

            if (windowRatio < srcRatio) {
                // Bind the height image to the height of the window
                destHeight = (int) ((double) width / (double) srcWidth * srcHeight);
            } else {
                // Bind the width of the image to the width of the window
                destWidth = (int) ((double) height / (double) srcHeight * srcWidth);
            }

            if (scalingType == Image.SCALE_FAST) {
                // seams faster this way than "Image.getScaledInstance"
                // Create a new BufferedImage and paint our source image to it
                BufferedImage destImage = new BufferedImage(destWidth,
                    destHeight, BufferedImage.TYPE_USHORT_565_RGB);
                Graphics g = destImage.getGraphics();
                g.drawImage(src, 0, 0, destWidth, destHeight, null);
                g.dispose();
                lastResizedImage = destImage;
                return destImage;
            }
            Image destImage = src.getScaledInstance(destWidth, destHeight,
                scalingType);
            lastResizedImage = destImage;
            return destImage;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception", e);
        }
        return null;
    }
}
