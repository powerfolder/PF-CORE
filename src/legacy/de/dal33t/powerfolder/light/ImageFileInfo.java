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
 * $Id: ImageFileInfo.java 7864 2009-05-02 20:37:15Z bytekeeper $
 */
package de.dal33t.powerfolder.light;

/**
 * A fileinfo object with some extra meta info about the image
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
@Deprecated
public class ImageFileInfo extends FileInfo {
    private int width = -1;
    private int height = -1;

    /** contructs a Image fileInfo object* */
    /*
     * public ImageFileInfo(Folder folder, File localFile) { super(folder,
     * localFile); if (localFile != null && localFile.exists()) { Dimension
     * resolution = ImageSupport.getResolution(localFile); if (resolution !=
     * null) { width = resolution.width; height = resolution.height; } } }
     */
    /**
     * @return Returns the height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Returns the width.
     */
    public int getWidth() {
        return width;
    }
}
