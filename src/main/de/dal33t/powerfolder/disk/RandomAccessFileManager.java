package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Reject;

/**
 * Handles RandomAccessFiles. Use this class if you want to prevent opening and
 * closing the same file frequently.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public class RandomAccessFileManager extends PFComponent {
    private Map<File, RandomAccessFile> files = new WeakHashMap<File, RandomAccessFile>();

    public RandomAccessFileManager(Controller controller) {
        super(controller);
    }

    /**
     * Returns a shared RandomAccessFile. When accessing the returned file, one
     * should synchronize on it.
     * 
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public RandomAccessFile getRandomAccessFile(File file)
        throws FileNotFoundException
    {
        synchronized (files) {
            RandomAccessFile raf = files.get(file);
            if (raf == null) {
                raf = new RandomAccessFile(file, "rw");
                files.put(file, raf);
            }
            return raf;
        }
    }

    /**
     * Force removal of this file. If the file was successfully removed, it will
     * be closed if possible.
     * <p>
     * TODO bytekeeper give this a more "correct" name. The file wouldn't
     * actually removed but the cached RAF file gets closed.
     * 
     * @param file
     */
    public void forceRemoveFile(File file) {
        Reject.ifNull(file, "File is null");

        RandomAccessFile raf;
        synchronized (file) {
            raf = files.get(file);
            if (raf != null) { // File is still there
                synchronized (file) {
                    files.remove(file);
                }
            }
        }
        if (raf != null) {
            synchronized (raf) {
                try {
                    raf.close();
                } catch (IOException e) {
                    log().error("Couldn't close file", e);
                }
            }
        }
    }
}
