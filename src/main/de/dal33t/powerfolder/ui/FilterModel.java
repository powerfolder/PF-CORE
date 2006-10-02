package de.dal33t.powerfolder.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * abstract class with commons for filtering, holds a searchField (ValueModel)
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public abstract class FilterModel extends PFComponent {
    /** the delay to use to give fast typers to complete their words */
    private static final long DELAY = 500;
    /** The task that performs the filtering */
    private TimerTask task = null;

    /** The value model of the searchfield we listen to */
    private ValueModel searchField;

    public FilterModel(Controller controller) {
        super(controller);
        setSearchField(new ValueHolder());
    }

    public abstract void reset();

    public abstract void filter();

    public ValueModel getSearchField() {
        return searchField;
    }

    public void setSearchField(ValueModel searchField) {
        this.searchField = searchField;
        searchField.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (task != null) {
                    task.cancel(); // cancel if seachfield changed before delay
                    // was reached
                }
                task = new TimerTask() {
                    public void run() {
                        filter();
                        task = null;
                    }
                };
                // schedule to filter after DELAY to make sure that fast typer
                // can complete their words before filtering
                getController().schedule(task, DELAY);
            }
        });
    }
}
