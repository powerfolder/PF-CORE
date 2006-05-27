package de.dal33t.powerfolder.ui.action;

import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * your inheriting class needs to implements those:<BR>
 * public void selectionChanged(SelectionChangeEvent e)<BR>
 * public void selectionsChanged(SelectionChangeEvent e)<BR>
 * public void actionPerformed(ActionEvent e)
 */
public abstract class SelectionBaseAction extends BaseAction implements
    SelectionChangeListener
{
    private SelectionModel selectionModel;

    protected SelectionBaseAction(String actionId, Controller controller,
        SelectionModel selectionModel)
    {
        super(actionId, controller);
        setSelectionModel(selectionModel);
    }

    protected SelectionBaseAction(String name, Icon icon,
        Controller controller, SelectionModel selectionModel)
    {
        super(name, icon, controller);
        setSelectionModel(selectionModel);
    }

    /** only set by constructor */
    private void setSelectionModel(SelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new NullPointerException("Selectionmodel is null");
        }
        this.selectionModel = selectionModel;
        selectionModel.addSelectionChangeListener(this);
    }

    public SelectionModel getSelectionModel() {
        return selectionModel;
    }
}
