package de.dal33t.powerfolder.util.ui;

import java.util.LinkedList;
import java.util.List;

/**
 * Class to keep track of selected objects. Call setSelection() to set a new
 * selection. Supportes single and multiple selection. Register listeners using
 * the addSelectionChangeListener method to recieve events if the selection has
 * changed.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5 $
 */
public class SelectionModel {

    /** The current selected items * */
    private Object[] selections;
    /** The previius selected items * */
    private Object[] oldSelections;

    /** The list of listeners to the selection changed events */
    private List<SelectionChangeListener> listeners = new LinkedList<SelectionChangeListener>();

    /** Create a Selection model */
    public SelectionModel() {
    }

    /** Set the current selection */
    public void setSelection(Object newSelection) {
        if (newSelection == null) {
            setSelections(null);
        } else {
            setSelections(new Object[]{newSelection});
        }
    }

    /** Set a list of selections */
    public void setSelections(Object[] newSelections) {
        this.oldSelections = selections;
        this.selections = newSelections;
        fireSelectionChanged();
    }

    /**
     * returns the selected object, if more are selected only the first is
     * returned. return null if nothing is selected
     */
    public Object getSelection() {
        if (selections == null || selections.length == 0) {
            return null;
        }
        return selections[0];
    }

    /**
     * Returns the selected objects. If nothing is selected null is returned.
     * Note that the items in the aray may be null
     */
    public Object[] getSelections() {
        return selections;
    }

    /** Returns the previous selection. */
    public Object getOldSelection() {
        if (oldSelections == null) {
            return null;
        }
        return oldSelections[0];
    }

    /** Returns the previous selections. */
    public Object[] getOldSelections() {
        return oldSelections;
    }

    /** register to recieve selection change events */
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        listeners.add(listener);
    }

    /** unregister this change listener */
    public void removeSelectionChangeListener(SelectionChangeListener listener)
    {
        listeners.remove(listener);
    }

    /** fires selectionChange events to all registered listeners */
    private void fireSelectionChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            SelectionChangeListener listener = listeners.get(i);
            listener
                .selectionChanged(new SelectionChangeEvent(this, selections));
        }
    }
}