package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

public class RandomAccessFileManager extends PFComponent {
	private Map<File, RandomAccessFile> files =
		new WeakHashMap<File, RandomAccessFile>();
	
	public RandomAccessFileManager(Controller controller) {
		super(controller);
	}

	/**
	 * Returns a shared RandomAccessFile.
	 * When accessing the returned file, one should synchronize on it.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 */
	public RandomAccessFile getRandomAccessFile(File file) throws FileNotFoundException {
		synchronized (files) {
			RandomAccessFile raf = files.get(file);
			if (raf == null) {
				raf = new RandomAccessFile(file, "rw");
				files.put(file, raf);
			}
			return raf;
		}
	}

	public void forceRemoveFile(File file) {
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
