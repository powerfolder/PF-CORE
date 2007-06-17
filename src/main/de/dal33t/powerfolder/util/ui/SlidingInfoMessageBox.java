package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;

/** 
 * Displays animated informational message box with OK button.
 * @see <code>Slider</code>
 * @author <a href="mailto:magapov@gmail.com">Maxim Agapov</a>
 * @version $Revision: 1.1 $
 */
public class SlidingInfoMessageBox implements PropertyChangeListener {
    
    private Slider slider;
    
    /**
     * @param message
     */
    public void show(String message) {
        if (slider == null) {
            JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
            JDialog dialog = optionPane.createDialog(null, "");
            JComponent contentPane = (JComponent) dialog.getContentPane();
            contentPane.setBorder (new LineBorder(Color.black, 1));
            slider = new Slider(contentPane);
            optionPane.addPropertyChangeListener (this);
            slider.show();
        }
    }
    
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange (PropertyChangeEvent pce) {
        if (pce.getPropertyName().equals (JOptionPane.VALUE_PROPERTY) && slider != null) {
            slider.close();
        }
    }
}
