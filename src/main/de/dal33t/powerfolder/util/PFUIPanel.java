package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.ui.UIPanel;

/**
 * A panel of powerfolder. basically provides access methods to
 * <code>Controller</code> and <code>UIController</code>.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class PFUIPanel extends PFUIComponent implements UIPanel {

    protected PFUIPanel(Controller controller) {
        super(controller);
    }
}
