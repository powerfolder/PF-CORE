package de.dal33t.powerfolder.light;

import java.awt.Dimension;
import java.io.File;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.ImageSupport;

/**
 * A fileinfo object with some extra meta info about the image
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class ImageFileInfo extends FileInfo {
    private int width = -1;
    private int height = -1;

    /** contructs a Image fileInfo object* */
    public ImageFileInfo(Folder folder, File localFile) {
        super(folder, localFile);
        if (localFile != null && localFile.exists()) {
            Dimension resolution = ImageSupport.getResolution(localFile);
            if (resolution != null) {
                width = resolution.width;
                height = resolution.height;
            }
        }
    }

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
