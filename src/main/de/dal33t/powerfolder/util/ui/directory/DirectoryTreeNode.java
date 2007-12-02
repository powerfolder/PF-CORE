package de.dal33t.powerfolder.util.ui.directory;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

/**
 * Class to represent a directory node in the tree.
 * Initially the node has a dummy node,
 * so that the node handle is displayed by the tree.
 * A node can be scanned, which removes the dummy node
 * and replaces with real file system subdirectories.
 */
public class DirectoryTreeNode extends DefaultMutableTreeNode {

    private boolean volume;
    private boolean scanned;

    /**
     * Constructor.
     *
     * @param directory
     * @param volume
     */
    public DirectoryTreeNode(File directory, boolean volume) {
        super(directory);
        this.volume = volume;
        if (volume) {
            add(new DefaultMutableTreeNode());
        } else if (directory != null && directory.isDirectory() && directory.canRead() && !directory.isHidden()) {

            // A quick peek.
            // If there are any subdirectories,
            // set scanned false and add a dummy,
            // deferring the real scan untll the node is expanded.
            // Otherwise if there are no readable directories,
            // set as scanned with no dummy node.
            scanned = true;
            
            // Patch for Windows Vista.
            // Vista may deny access to directories
            // and this results in a null file list.
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f != null && f.canRead() &&
                            f.isDirectory() && !f.isHidden()) {
                        add(new DefaultMutableTreeNode());
                        scanned = false;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Scans the node directory.
     * Remove any dummy node,
     * and add nodes for any subdirectories.
     */
    public void scan() {
        while (getChildCount() > 0) {
            remove(0);
        }
        File f = (File) getUserObject();
        if (f != null && f.isDirectory() && f.canRead()) {
            for(File f2: f.listFiles()) {
                if (f2.isDirectory() && !f2.isHidden() && f2.canRead()) {
                    DirectoryTreeNode dtn2 = new DirectoryTreeNode(f2, false);
                    add(dtn2);
                }
            }
        }
        scanned = true;
    }

    public void unscan() {
        scanned = false;
    }

    /**
     * Whether the node has been scanned.
     *
     * @return
     */
    public boolean isScanned() {
        return scanned;
    }

    /**
     * Whether the node is a base file system volume.
     *
     * @return
     */
    public boolean isVolume() {
        return volume;
    }
}
