/* $Id: QuickInfoPanel.java,v 1.2 2006/03/06 00:15:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;

/**
 * The panel the contains the most important and concentrated information
 * about a element (e.g. Folder)
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class QuickInfoPanel extends PFUIComponent {
    private JPanel panel;
    
    private JComponent logo;
    private JComponent headerText;
    private JComponent info1Text;
    private JComponent info2Text;

    protected QuickInfoPanel(Controller controller) {
        super(controller);
    }
    
    /**
     * Create the top part of the panel which contains the most concentrated
     * informations
     * 
     * @return
     */
    public JComponent getUIComponent() {
        if (panel == null) {
            // Init components
            initComponents();
            // Init general components
            initComponents0();
            
            // Build ui
            FormLayout layout = new FormLayout("pref, 14dlu, pref",
                "top:pref, 7dlu, pref, 3dlu, top:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);
            CellConstraints cc = new CellConstraints();
            builder.add(logo, cc.xywh(1, 1, 1, 5));
            builder.add(headerText, cc.xy(3, 1));

            builder.add(info1Text, cc.xywh(3, 3, 1, 1));
            builder.add(info2Text, cc.xywh(3, 5, 1, 1));

            panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    /**
     * Initalizes the components
     * @return
     */
    private void initComponents0() {
        headerText = getHeaderText();
        info1Text = getInfoText1();
        info2Text = getInfoText2();
        logo = getPicto();
    }

    // Implementing part ******************************************************

    /**
     * Overwrite if you want to initalize components before the other JComponent
     * getters are getting called
     */
    protected void initComponents() {
    }
    
    /**
     * Returns the picto for this panel. Displayed on the left upper side
     * @return
     */
    protected abstract JComponent getPicto();
    
    /**
     * The header text. Upper text. Should usually a bigger font
     * @return
     */
    protected abstract JComponent getHeaderText();
    
    /**
     * First line of info. Use a bigger, but not to big font
     * @return
     */
    protected abstract JComponent getInfoText1();
    
    /**
     * First line of info. Use a bigger, but not to big font
     * @return
     */
    protected abstract JComponent getInfoText2();
    
}
