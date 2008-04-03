package de.dal33t.powerfolder.util.ui;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.util.Translation;

/**
 * A dialog which allows the user to select one of the following options:
 *  <ul>
 *      <li> Choose a random port and continue </li>
 *      <li> Continue without opening a listener </li>
 *      <li> Exit the program </li>
 *  </ul>
 * @author Dennis "Bytekeeper" Waldherr
 */
public class PortBindingErrorDialog {
    public enum Option {
        IGNORE("dialog.binderror.option.ignore"),
        EXIT("dialog.binderror.option.exit");
        
        private final String translationKey;

        Option(String tkey) {
            this.translationKey = tkey;
        }
        
        @Override
        public String toString() {
            return Translation.getTranslation(translationKey);
        }
    }

    private EnumOptionPane<Option> cop;
    
    /**
     * Shows a dialog where the user can select what he wants to be done. 
     */
    public void show() {
        JOptionPane optionPane = new JOptionPane(Translation
            .getTranslation("dialog.binderror.option.text"));
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        
        cop = new EnumOptionPane<Option>(optionPane, 
            Translation.getTranslation("dialog.binderror.option.title"));
        cop.setDefaultOption(Option.IGNORE);
        cop.setOptions(new Option[] { Option.IGNORE, Option.EXIT});
        cop.show();
    }

    /**
     * Returns the option the user selected
     * @return
     */
    public Option getSelectedOption() {
        return cop.getValue();
    }
    
    /**
     * DON'T CALL, FOR TESTING PURPOSES ONLY"
     */
    public JDialog getDialog() {
        return cop != null ? cop.getDialog() : null;
    }
}
