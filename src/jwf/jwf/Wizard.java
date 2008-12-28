package jwf;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.ui.Icons;

/**
 * This class controls a wizard.
 * <p>
 * Add it to a frame or any other container then call start with your initial
 * wizard panel.
 * <p>
 * Listeners can also be added to trap when the wizard finishes and when the
 * wizard is cancelled.
 * 
 * @author Christopher Brind
 */
public class Wizard extends JPanel implements ActionListener {
    public static final String WIZARD_ATTRIBUTE = "wizard";

    public static final String BACK_I18N = "BACK_I18N";
    public static final String NEXT_I18N = "NEXT_I18N";
    public static final String FINISH_I18N = "FINISH_I18N";
    public static final String CANCEL_I18N = "CANCEL_I18N";
    public static final String HELP_I18N = "HELP_I18N";
    public static final String BACK_I18N_DESCRIPTION = "BACK_I18N_DESCRIPTION";
    public static final String NEXT_I18N_DESCRIPTION = "NEXT_I18N_DESCRIPTION";
    public static final String FINISH_I18N_DESCRIPTION = "FINISH_I18N_DESCRIPTION";
    public static final String CANCEL_I18N_DESCRIPTION = "CANCEL_I18N_DESCRIPTION";
    public static final String HELP_I18N_DESCRIPTION = "HELP_I18N_DESCRIPTION";
    public static final Dimension WIZARD_DEFAULT_WINDOW_SIZE = new Dimension(
        650, 450);
    public static final Dimension WIZARD_MAC_WINDOW_SIZE = new Dimension(750,
        500);

    private final JButton backButton = new JButton("< Back");
    private final JButton nextButton = new JButton("Next >");
    private final JButton finishButton = new JButton("Finish");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton helpButton = new JButton("Help");

    private final Set<WizardListener> listeners = new TreeSet<WizardListener>();

    private Stack<WizardPanel> previous;
    private WizardPanel current;
    private WizardContext ctx;

    /** Creates a new wizard. */
    public Wizard() {
        init();
    }

    private void init() {
        ctx = new WizardContext();

        nextButton.addActionListener(this);
        backButton.addActionListener(this);
        finishButton.addActionListener(this);
        cancelButton.addActionListener(this);
        helpButton.addActionListener(this);

        nextButton.setEnabled(false);
        backButton.setEnabled(false);
        finishButton.setEnabled(false);
        cancelButton.setEnabled(false);
        helpButton.setEnabled(false);

        setLayout(new BorderLayout());

        ButtonBarBuilder barBuilder = ButtonBarBuilder
            .createLeftToRightBuilder();
        barBuilder.addGridded(backButton);
        barBuilder.addRelatedGap();
        barBuilder.addGridded(nextButton);
        barBuilder.addUnrelatedGap();
        barBuilder.addGridded(cancelButton);
        barBuilder.addRelatedGap();
        barBuilder.addGridded(finishButton);

        JComponent navButtons = barBuilder.getPanel();
        JComponent helpButtons = ButtonBarFactory.buildCenteredBar(helpButton);

        navButtons.setBorder(Borders.DLU2_BORDER);
        helpButtons.setBorder(Borders.DLU2_BORDER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BorderLayout());
        buttons.add(new JSeparator(), BorderLayout.NORTH);
        buttons.add(helpButtons, BorderLayout.WEST);
        buttons.add(navButtons, BorderLayout.EAST);

        // buttons

        add(buttons, BorderLayout.SOUTH);

        if (OSUtil.isMacOS()) {
            setMinimumSize(WIZARD_MAC_WINDOW_SIZE);
            setPreferredSize(WIZARD_MAC_WINDOW_SIZE);
        } else {
            setMinimumSize(WIZARD_DEFAULT_WINDOW_SIZE);
            setPreferredSize(WIZARD_DEFAULT_WINDOW_SIZE);
        }
    }

    /*
     * Sets a map of labels for changing the labels of the wizard buttons The
     * keys are the I18N constants of the Wizard class @param map A Map object
     * containing 5 key-value elements
     */
    public void setI18NMap(Map<String, String> map) {
        if (!map.isEmpty()) {
            nextButton.setText(map.get(NEXT_I18N));
            nextButton.setToolTipText(map.get(NEXT_I18N_DESCRIPTION));
            nextButton.setIcon(Icons.ARROW_RIGHT);

            backButton.setText(map.get(BACK_I18N));
            backButton.setToolTipText(map.get(BACK_I18N_DESCRIPTION));
            backButton.setIcon(Icons.ARROW_LEFT);

            backButton.setActionCommand("< Back");
            nextButton.setActionCommand("Next >");
            finishButton.setActionCommand("Finish");
            cancelButton.setActionCommand("Cancel");
            helpButton.setActionCommand("Help");

            cancelButton.setText(map.get(CANCEL_I18N));
            cancelButton.setToolTipText(map.get(CANCEL_I18N_DESCRIPTION));

            helpButton.setText(map.get(HELP_I18N));
            helpButton.setToolTipText(map.get(HELP_I18N_DESCRIPTION));
            helpButton.setIcon(Icons.QUESTION);
        }
    }

    /**
     * Add a listener to this wizard.
     * 
     * @param listener
     *            a WizardListener object
     */
    public void addWizardListener(WizardListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from this wizard.
     * 
     * @param listener
     *            a WizardListener object
     */
    public void removeWizardListener(WizardListener listener) {
        listeners.remove(listener);
    }

    /** Start this wizard with this panel. */
    public void start(WizardPanel wp, boolean resetContext) {
        previous = new Stack<WizardPanel>();
        if (resetContext) {
            ctx = new WizardContext();
        }
        ctx.setAttribute(WIZARD_ATTRIBUTE, this);
        wp.setWizardContext(ctx);
        setPanel(wp);
        updateButtons();
        setupMenu();
    }

    /**
     * Hide a menu so that it responds to accelerator keys of actions.
     */
    private void setupMenu() {
        JMenuBar mb = new JMenuBar() {
            public Dimension getPreferredSize() {
                return new Dimension((int) super.getPreferredSize().getWidth(),
                    0);
            }
        };
        add(mb, BorderLayout.NORTH);
        Action helpAction = new MyHelpAction();
        mb.add(new JMenuItem(helpAction));
    }

    /**
     * @return the currently active context.
     */
    public WizardContext getContext() {
        return ctx;
    }

    /**
     * Handle's button presses. param ae an ActionEvent object
     */
    public void actionPerformed(ActionEvent e) {

        String ac = e.getActionCommand();
        if ("< Back".equals(ac)) {
            back();
        } else if ("Next >".equals(ac)) {
            next();
        } else if ("Finish".equals(ac)) {
            finish();
        } else if ("Cancel".equals(ac)) {
            cancel();
        } else if ("Help".equals(ac)) {
            help();
        }

    }

    private void setPanel(WizardPanel wp) {
        if (null != current) {
            remove(current);
        }

        current = wp;
        if (null == current) {
            current = new NullWizardPanel();
        }
        add(current, BorderLayout.CENTER);

        for (WizardListener listener : listeners) {
            listener.wizardPanelChanged(this);
        }
        setVisible(true);
        revalidate();
        updateUI();
        current.display();
    }

    void updateButtons() {
        cancelButton.setEnabled(true);
        helpButton.setEnabled(current.hasHelp());
        backButton.setEnabled(!previous.isEmpty());
        nextButton.setEnabled(current.hasNext());
        finishButton.setEnabled(current.canFinish());
    }

    /**
     * Moves the wizard to the previous panel.
     * <p>
     * Basically does the same link pressing "< Back"
     */
    public void back() {
        WizardPanel wp = previous.pop();
        setPanel(wp);
        updateButtons();

    }

    /**
     * Tries to move the wizard to the next panel.
     * <p>
     * Basically does the same link pressing "Next >"
     */
    public void next() {
        List<WizardPanel> list = new ArrayList<WizardPanel>();
        if (current.validateNext(list)) {
            previous.push(current);
            WizardPanel wp = current.next();
            if (null != wp) {
                wp.setWizardContext(ctx);
            }

            setPanel(wp);
            updateButtons();
        } else {
            if (!list.isEmpty()) {
                showErrorMessages(list);
            }
        }
    }
    
    public JButton getNextButton() {
        return nextButton;
    }
    
    private void finish() {

        List<WizardPanel> list = new ArrayList<WizardPanel>();
        if (current.validateFinish(list)) {
            current.finish();
            for (WizardListener listener : listeners) {
                listener.wizardFinished(this);
            }
        } else {
            showErrorMessages(list);
        }
    }

    private void cancel() {

        for (WizardListener listener : listeners) {
            listener.wizardCancelled(this);
        }
    }

    private void help() {
        current.help();
    }

    private void showErrorMessages(List<WizardPanel> list) {
        Window w = SwingUtilities.windowForComponent(this);
        ErrorMessageBox errorMsgBox;

        if (w instanceof Frame) {
            errorMsgBox = new ErrorMessageBox((Frame) w);
        } else if (w instanceof Dialog) {
            errorMsgBox = new ErrorMessageBox((Dialog) w);
        } else {
            errorMsgBox = new ErrorMessageBox();
        }

        errorMsgBox.showErrorMessages(list);
    }

    private class MyHelpAction extends AbstractAction {
        private MyHelpAction() {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_8,
                ActionEvent.ALT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            help();
        }
    }
}
