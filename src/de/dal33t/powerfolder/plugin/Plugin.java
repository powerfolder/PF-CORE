package de.dal33t.powerfolder.plugin;

import javax.swing.JFrame;

public interface Plugin {
    public String getName();
    
    public String getDescription();
  
    public void start();

    public void stop();

    public boolean hasOptionsFrame();

    public void showOptionsFrame(JFrame parent);
}
