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
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.net.NodeList;
import org.apache.commons.cli.Options;

import javax.swing.JFileChooser;
import java.io.File;
import java.io.IOException;

public class NodeListEditor {
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
