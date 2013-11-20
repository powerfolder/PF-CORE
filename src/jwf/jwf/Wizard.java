package jwf;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;
import de.dal33t.powerfolder.util.os.OSUtil;

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

    // Wizard sizes
    public static final Dimension WIZARD_TINY_WINDOW_SIZE = new Dimension(400,
        355);
    public static final Dimension WIZARD_TINY_MAC_WINDOW_SIZE = new Dimension(400,
        355);

    public static final Dimension WIZARD_BIG_WINDOW_SIZE = new Dimension(
        650, 480);
    public static final Dimension WIZARD_BIG_MAC_WINDOW_SIZE = new Dimension(750,
        540);

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

    private final JButton backButton = new JButton("< Back");
    private final JButton nextButton = new JButton("Next >");
    private final JButton finishButton = new JButton("Finish");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton helpButton = new JButton("Help");

    private final Set<WizardListener> listeners = new HashSet<WizardListener>();

    private Stack<WizardPanel> previous;
    private WizardPanel current;
    private WizardContext ctx;

    /** Creates a new wizard. */
    public Wizard(boolean tiny) {
        init(tiny);
    }
    
    public boolean isTiny() {
        return getPreferredSize().equals(WIZARD_TINY_WINDOW_SIZE);
    }

    private void init(boolean tiny) {
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
        if (!tiny) {
            barBuilder.addRelatedGap();
            barBuilder.addGridded(finishButton);
        }

        JComponent navButtons = barBuilder.getPanel();
        navButtons.setOpaque(false);
        JComponent helpButtons = ButtonBarFactory.buildCenteredBar(helpButton);
        helpButtons.setOpaque(false);

        navButtons.setBorder(Borders.DLU2_BORDER);
        helpButtons.setBorder(Borders.DLU2_BORDER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BorderLayout());
        // buttons.add(new JSeparator(), BorderLayout.NORTH);
        buttons.add(helpButtons, BorderLayout.WEST);
        buttons.add(navButtons, BorderLayout.EAST);

        // buttons

        add(buttons, BorderLayout.SOUTH);

        if (tiny) {
            if (OSUtil.isMacOS()) {
                setMinimumSize(WIZARD_TINY_MAC_WINDOW_SIZE);
                setPreferredSize(WIZARD_TINY_MAC_WINDOW_SIZE);
            } else {
                setMinimumSize(WIZARD_TINY_WINDOW_SIZE);
                setPreferredSize(WIZARD_TINY_WINDOW_SIZE);
            }
        } else {
            if (OSUtil.isMacOS()) {
                setMinimumSize(WIZARD_BIG_MAC_WINDOW_SIZE);
                setPreferredSize(WIZARD_BIG_MAC_WINDOW_SIZE);
            } else {
                setMinimumSize(WIZARD_BIG_WINDOW_SIZE);
                setPreferredSize(WIZARD_BIG_WINDOW_SIZE);
            }
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
            nextButton.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
            nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
            nextButton.setActionCommand("Next >");

            backButton.setText(map.get(BACK_I18N));
            backButton.setToolTipText(map.get(BACK_I18N_DESCRIPTION));
            backButton.setIcon(Icons.getIconById(Icons.ARROW_LEFT));
            backButton.setActionCommand("< Back");

            finishButton.setText(map.get(FINISH_I18N));
            finishButton.setToolTipText(map.get(FINISH_I18N_DESCRIPTION));
            finishButton.setMinimumSize(nextButton.getMinimumSize());
            finishButton.setMaximumSize(nextButton.getMaximumSize());
            finishButton.setPreferredSize(nextButton.getPreferredSize());
            finishButton.setActionCommand("Finish");

            cancelButton.setText(map.get(CANCEL_I18N));
            cancelButton.setToolTipText(map.get(CANCEL_I18N_DESCRIPTION));
            cancelButton.setActionCommand("Cancel");
            cancelButton.setMinimumSize(nextButton.getMinimumSize());
            cancelButton.setMaximumSize(nextButton.getMaximumSize());
            cancelButton.setPreferredSize(nextButton.getPreferredSize());

            helpButton.setText(map.get(HELP_I18N));
            helpButton.setToolTipText(map.get(HELP_I18N_DESCRIPTION));
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
        ctx.setAttribute(WizardContextAttributes.WIZARD_ATTRIBUTE, this);
        wp.setWizardContext(ctx);
        setPanel(wp);
        updateButtons();
        setupMenu();
    }

    /**
     * Hide a menu so that it responds to accelerator keys of actions.
     */
    private void setupMenu() {
        JMenuBar mb = new MyJMenuBar();
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
        enableButton(cancelButton, current.canCancel());
        enableButton(helpButton, current.hasHelp());
        enableButton(backButton, !previous.isEmpty()
            && previous.peek().canGoBackTo());
        enableButton(finishButton, current.canFinish());

        // If next has focus and is about to go disabled, loose focus.
        if (nextButton.hasFocus() && !current.hasNext()) {
            helpButton.requestFocus();
        } else if (finishButton.hasFocus() && !current.canFinish()) {
            finishButton.requestFocus();
        }

        enableButton(nextButton, current.hasNext());
    }

    private static void enableButton(JButton button, boolean enable) {
        button.setEnabled(enable);
        button.setVisible(enable);
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
        Cursor c = CursorUtils.setWaitCursor(this);
        try {
            if (current.validateNext()) {
                previous.push(current);
                WizardPanel wp = current.next();
                if (null != wp) {
                    wp.setWizardContext(ctx);
                }

                setPanel(wp);
                updateButtons();
            }
        } finally {
            CursorUtils.returnToOriginal(this, c);
        }
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public WizardPanel getCurrentPanel() {
        return current;
    }

    private void finish() {

        if (current.validateFinish()) {
            current.finish();
            for (WizardListener listener : listeners) {
                listener.wizardFinished(this);
            }
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

    private class MyHelpAction extends AbstractAction {
        private MyHelpAction() {
            putValue(ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_8, ActionEvent.ALT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            help();
        }
    }

    private static class MyJMenuBar extends JMenuBar {
        public Dimension getPreferredSize() {
            return new Dimension((int) super.getPreferredSize().getWidth(), 0);
        }
    }
}
