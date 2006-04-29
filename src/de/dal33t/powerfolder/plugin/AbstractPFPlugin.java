package de.dal33t.powerfolder.plugin;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;



public abstract class AbstractPFPlugin extends PFComponent implements Plugin {
    
    public AbstractPFPlugin(Controller controller) {
        super(controller);        
    }
}
