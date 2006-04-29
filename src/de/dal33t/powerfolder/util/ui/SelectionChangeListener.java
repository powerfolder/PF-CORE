package de.dal33t.powerfolder.util.ui;

import java.util.EventListener;

/**
 * To recieve events from the SelectionModel implement this interface.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public interface SelectionChangeListener extends EventListener {
    /** called if a selection has changed in the SelectionModel */
    public void selectionChanged(SelectionChangeEvent event);

}
