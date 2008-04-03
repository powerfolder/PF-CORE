package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A helper class which addds enums options and default option (aka default button) to the {@link JOptionPane} class.  
 * 
 * @author Dennis "Bytekeeper" Waldherr
 *
 * @param <T>
 */
public class EnumOptionPane<T extends Enum<T>> {
    protected final JOptionPane optionPane;
    protected T[] options;
    private T defaultOption;
    private final String title;
    private JDialog dialog;
    
    public EnumOptionPane(JOptionPane optionPane, String title) {
        this.optionPane = optionPane;
        this.title = title;
    }
    
    public void show() {
        if (options == null) {
            throw new NullPointerException("Options not set!");
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Object[] buttons = new Object[options.length];
                    JButton defaultButton = null;
                    for (int i = 0; i < buttons.length; i++) {
                        JButton button = createButton(options[i]); 
                        buttons[i] = button;
                        if (defaultOption == options[i]) {
                            defaultButton = button;
                        }
                    }
                    optionPane.setOptions(buttons);
                    
                    dialog = optionPane.createDialog(title);
                    dialog.getRootPane().setDefaultButton(defaultButton);
                    dialog.setVisible(true);
                    
                    if (optionPane.getValue() == null && defaultOption != null) {
                        optionPane.setValue(defaultOption);
                    }
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }        
            
    }
    
    /**
     * Returns the selected option.
     * @return the option selected or null if no option was selected and no default option was given.
     */
    @SuppressWarnings("unchecked")
    public T getValue() {
        if (optionPane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
            throw new IllegalStateException("Options not initialized!");
        }
        return (T) optionPane.getValue();
    }
    
    /**
     * Sets the default option.
     * Implicitly sets the default button. If the given option is not in the
     * set of options available for selection it will only returned if the user closed the dialog. 
     * @param option
     */
    public void setDefaultOption(T option) {
        this.defaultOption = option;
    }
    
    /**
     * Set the options that should be available for selection.
     * @param options
     */
    public void setOptions(T[] options) {
        this.options = Arrays.copyOf(options, options.length);
    }
    

    /**
     * DON'T CALL, FOR TESTING PURPOSES ONLY"
     */
    public JDialog getDialog() {
        return dialog;
    }
    
    // Internal stuff
    private JButton createButton(final T option) {
        JButton button = new JButton(option.toString());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(option);
            }
        });
        return button;
    }
}
