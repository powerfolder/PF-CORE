package de.dal33t.powerfolder.ui.model;

import javax.swing.AbstractListModel;

import de.dal33t.powerfolder.disk.Blacklist;

/**
 * maps the current blacklist to a ListModel.
 * <p>
 * May contain <code>null</code> as blacklist = empty listmodel
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class BlackListPatternsListModel extends AbstractListModel {
    private Blacklist blacklist;
    private int oldSize;

    public BlackListPatternsListModel(Blacklist aBlacklist) {
        setBlacklist(aBlacklist);
    }

    public void setBlacklist(Blacklist aBlacklist) {
        blacklist = aBlacklist;
    }

    public Object getElementAt(int index) {
        return blacklist.getPatterns().get(index);
    }

    public int getSize() {
        if (blacklist == null) {
            return 0;
        }
        return blacklist.getPatterns().size();

    }

    /** why can't i fire a complete change? This is a hack. */
    public void fireUpdate() {
        fireContentsChanged(this, 0, oldSize + 1);
        fireContentsChanged(this, 0, blacklist.getPatterns().size() + 1);
        oldSize = blacklist.getPatterns().size();
    }
}