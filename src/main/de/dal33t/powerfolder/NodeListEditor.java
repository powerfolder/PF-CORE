package de.dal33t.powerfolder;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.apache.commons.cli.Options;

import de.dal33t.powerfolder.light.NodeList;
import de.dal33t.powerfolder.util.Loggable;

public class NodeListEditor extends Loggable {
    private NodeList nodeList;
    
    private JFileChooser nodeFileChooser;
    
    public static void main(String... args) {
        NodeListEditor nle = new NodeListEditor();
        Options opt = new Options();
    }
    
    private NodeListEditor() {
        nodeFileChooser = new JFileChooser();
        nodeFileChooser.setMultiSelectionEnabled(false);
        
        nodeList = new NodeList();
    }
    
    /**
     * Loads the nodelist from the given file or from user selection
     * @param file a suggested file, if null a JFileChooser is shown to the user
     * @param userSelection if true or the file parameter is null, the user may select the file to load.
     *      If he cancels nothing happens.
     * @throws IOException
     */
    private void load(File file, boolean userSelection) throws IOException, ClassNotFoundException {
        if (file == null || userSelection) {
            if (file != null) {
                nodeFileChooser.setSelectedFile(file);
            }
            if (nodeFileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            file = nodeFileChooser.getSelectedFile();
        }
        
        synchronized (nodeList) {
            nodeList.load(file);
        }
    }
    
    /**
     * Saves the nodelist to a given file or to a user selected file.
     * @param file a suggested file, if null a JFileChooser is shown to the user
     * @param userSelection if true or the file parameter is null, the user may select the file to save to.
     *      If he cancels nothing happens.
     * @throws IOException
     */
    private void save(File file, boolean userSelection) throws IOException {
        if (file == null || userSelection) {
            if (file != null) {
                nodeFileChooser.setSelectedFile(file);
            }
            if (nodeFileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            file = nodeFileChooser.getSelectedFile();
        }
        
        synchronized (nodeList) {
            nodeList.save(file);
        }
    }
}
