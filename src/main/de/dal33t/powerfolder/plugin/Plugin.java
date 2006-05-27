package de.dal33t.powerfolder.plugin;

import javax.swing.JDialog;

/**
 * TODO SCHAASTER add javadoc
 */
public interface Plugin {
    public String getName();
    
    public String getDescription();
  
    public void start();

    public void stop();

    public boolean hasOptionsDialog();

    public void showOptionsDialog(JDialog parent);
}
