package de.dal33t.powerfolder.util.ui;

import java.util.EventObject;

/**
 * Event fired if a selection has changed in the SelectionModel
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class SelectionChangeEvent extends EventObject {

    private Object[] selections;

    /** package protected. Only created by SelectionModel */
    SelectionChangeEvent(Object source, Object[] selections) {
        super(source);
        this.selections = selections;
    }

    /** returns the new selection (maybe null) */
    public Object getSelection() {
        if (selections == null || selections.length == 0) {
            return null;
        }
        return selections[0];
    }

    /** Returns the new list of selections (maybe null, or items maybe null) */
    public Object[] getSelections() {
        return selections;
    }
}
