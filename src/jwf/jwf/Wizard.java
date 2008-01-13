package jwf;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

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
    public static final Dimension WIZARD_WINDOW_SIZE = new Dimension(600, 450);

    private final JButton backButton = new JButton("< Back");
    private final JButton nextButton = new JButton("Next >");
    private final JButton finishButton = new JButton("Finish");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton helpButton = new JButton("Help");

    private final HashMap listeners = new HashMap();

    private Stack previous = null;
    private WizardPanel current = null;
    private WizardContext ctx = null;
    private Map i18n = null;

    public Wizard(Map i18n) {
        this.i18n = i18n;
        init();
        this.applyI18N(this.i18n);
    }

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

        setMinimumSize(WIZARD_WINDOW_SIZE);
        setPreferredSize(WIZARD_WINDOW_SIZE);
    }

    /*
     * Sets a map of labels for changing the labels of the wizard buttons The
     * keys are the I18N constants of the Wizard class @param map A Map object
     * containing 5 key-value elements
     */
    public void setI18NMap(Map map) {
        i18n = map;
        applyI18N(i18n);
    }

    private void applyI18N(Map map) {
        if (!map.isEmpty()) {
            nextButton.setText((String) map.get(NEXT_I18N));
            backButton.setText((String) map.get(BACK_I18N));
            finishButton.setText((String) map.get(FINISH_I18N));
            cancelButton.setText((String) map.get(CANCEL_I18N));
            helpButton.setText((String) map.get(HELP_I18N));
            nextButton.setToolTipText((String) map.get(NEXT_I18N_DESCRIPTION));
            backButton.setToolTipText((String) map.get(BACK_I18N_DESCRIPTION));
            finishButton.setToolTipText((String) map.get(FINISH_I18N_DESCRIPTION));
            cancelButton.setToolTipText((String) map.get(CANCEL_I18N_DESCRIPTION));
            helpButton.setToolTipText((String) map.get(HELP_I18N_DESCRIPTION));

            backButton.setActionCommand("< Back");
            nextButton.setActionCommand("Next >");
            finishButton.setActionCommand("Finish");
            cancelButton.setActionCommand("Cancel");
            helpButton.setActionCommand("Help");

        }
    }

    /**
     * Add a listener to this wizard.
     * 
     * @param listener
     *            a WizardListener object
     */
    public void addWizardListener(WizardListener listener) {
        listeners.put(listener, listener);
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
        previous = new Stack();
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
                return new Dimension((int) super.getPreferredSize().getWidth(), 0);
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
    public void actionPerformed(ActionEvent ae) {

        String ac = ae.getActionCommand();
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

        Iterator iter = listeners.values().iterator();
        while (iter.hasNext()) {
            WizardListener listener = (WizardListener) iter.next();
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
        backButton.setEnabled(previous.size() > 0);
        nextButton.setEnabled(current.hasNext());
        finishButton.setEnabled(current.canFinish());
    }

    private void back() {
        WizardPanel wp = (WizardPanel) previous.pop();
        setPanel(wp);
        updateButtons();

    }

    /**
     * Tries to move the wizard to the next panel.
     * <p>
     * Basically does the same link pressing "Next >"
     */
    public void next() {
        ArrayList list = new ArrayList();
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

    private void finish() {

        ArrayList list = new ArrayList();
        if (current.validateFinish(list)) {
            current.finish();
            Iterator iter = listeners.values().iterator();
            while (iter.hasNext()) {
                WizardListener listener = (WizardListener) iter.next();
                listener.wizardFinished(this);
            }
        } else {
            showErrorMessages(list);
        }
    }

    private void cancel() {

        Iterator iter = listeners.values().iterator();
        while (iter.hasNext()) {
            WizardListener listener = (WizardListener) iter.next();
            listener.wizardCancelled(this);
        }
    }

    private void help() {
        current.help();
    }

    private void showErrorMessages(ArrayList list) {
        Window w = SwingUtilities.windowForComponent(this);
        ErrorMessageBox errorMsgBox = null;

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
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_8, ActionEvent.ALT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            help();
        }
    }
}
