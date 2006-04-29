/* $Id: DynDnsValidator.java,v 1.4 2005/11/04 14:04:51 schaatser Exp $
 */
package de.dal33t.powerfolder.net;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.ConnectionListener;

/**
 * 
 * @author albena
 *
 * DynDnsValidator class provides means for DynDns validation
 * as well as some UI utility methods
 */
public class DynDnsValidator extends PFComponent {

    private JDialog uiComponent;

    public DynDnsValidator(Controller controller){
        super(controller);
    }
    
    /**
     * Validates given dynDns for compatibility with the current host
     * @param dynDns to validate
     * @return true if validation succeeded, false otherwise
     */
    public boolean validateDynDns(String dynDns){
                
        // validates the dynamic dns entry if there is one entered
        if (!StringUtils.isBlank(dynDns)) {
            if (getController().getConnectionListener() != null) {

                // sets the new dyndns with validation enabled
                int res = getController().getConnectionListener().setMyDynDns(dynDns, true);                         
              
                // check the result from validation
                switch ( res ) {
                	case ConnectionListener.VALIDATION_FAILED:
                	    
                	    // validation failed ask the user if he/she
                	    // wants to continue with these settings
                	     
                	int result = JOptionPane
                        .showConfirmDialog(
                                getController().getUIController().getMainFrame().getUIComponent(),
                            "The entered DynDns does not match to this host. \n " +
                            "Are you sure you want to continue with it? "
                             ,
                             "The entered DynDns does not match to this host.", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                            
                	    if ( result == JOptionPane.YES_OPTION) {
                	        // the user is happy with his/her settings, then
                	        // set the new dyndns without further validation                	        
                	        getController().getConnectionListener().setMyDynDns(dynDns, false);                         
                	    }
                	    else {
                	        // the user wants to change the dyndns settings
                	        getController().getConnectionListener().setMyDynDns(null, false);    
                	        return false;
                	    }
                	    break;
                	case ConnectionListener.CANNOT_RESOLVE:
                	    // the new dyndns could not be resolved
                	    // force the user to enter a new one
                	    getController().getConnectionListener().setMyDynDns(null, false);
                	    return false;
                	    
                	case ConnectionListener.OK:
                	    // Okay is good ! user does not need to be informed
                //                	    getController().getUIController()
                //            	    	.showMessage(null,
                //            	    	        "Success",
                //            	    	        Translation.getTranslation("preferences.dialog.statusDynDnsSuccess",
                // dynDns));
                }
            }
            
        } else {
            // just resets the dyndns entry
            if (getController().getConnectionListener() != null) {
              getController().getConnectionListener().setMyDynDns(null, false); 
            
            }
        }
        // all validations have passed
        return true;
    }
    
    /**
     * Shows warning message to the user in case the validation goes wrong 
     * @param type of validation failure 
     * @param arg additional argument (dyndns)
     */
    public void showWarningMsg(int type, String arg){
        switch(type) {
        	case ConnectionListener.VALIDATION_FAILED:
        	    getController().getUIController()
        	    	.showWarningMessage(
        	    	        "Warning",
        	    	        Translation.getTranslation("preferences.dialog.statusValidFailed", arg));
        	break;
        
        	case ConnectionListener.CANNOT_RESOLVE:
        	    getController().getUIController()
        	    	.showWarningMessage(
        	    	         "Warning",
        	    	         Translation.getTranslation("preferences.dialog.statusValidFailed", arg));
        }
   }
   
   /**
    * close the wait message box 
    *
    */ 
   public final void close() {
        log().verbose("Close called: " + this);
        if (uiComponent != null) {
            uiComponent.dispose();
            uiComponent = null;
        }
    }
    
    /**
     * Shows (and builds) the wait message box
     */
    public final void show(String dyndns) {
        log().verbose("Open called: " + this);
        getUIComponent(dyndns).setVisible(true);
    }
    
    /**
     * retrieves the title of the message box
     * @return
     */
    private String getTitle() {
        return "processing...";
    }
    
    /**
     * Setups the UI for the wait message box
     * @param dyndns
     * @return
     */
    protected final JDialog getUIComponent(String dyndns) {
        if (uiComponent == null) {
            log().verbose("Building ui component for " + this);
            uiComponent = new JDialog(getController().getUIController().getMainFrame().getUIComponent(), 
                  				  getTitle());
            uiComponent.setResizable(false);
            
            uiComponent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            
    
            FormLayout layout = new FormLayout("pref, 14dlu, pref:grow",
                "pref, pref:grow, 7dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);
           
            CellConstraints cc = new CellConstraints();

            // Build
            int xpos = 1, ypos = 1, wpos = 1, hpos = 1;
            builder.add(new JLabel(Translation.getTranslation("preferences.dialog.statusWaitDynDns", dyndns)), cc.xywh(xpos, ypos, wpos, hpos));
   
            // Add panel to component
            uiComponent.getContentPane().add(builder.getPanel());
      
            uiComponent.pack();
            Component parent = uiComponent.getOwner();
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;
            uiComponent.setLocation(x, y);
        }
        return uiComponent;
    }    
}