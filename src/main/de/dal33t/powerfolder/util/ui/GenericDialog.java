package de.dal33t.powerfolder.util.ui;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import static de.dal33t.powerfolder.util.ui.GenericDialogType.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Replacement class for JOptionPane.
 */
public class GenericDialog {

    /** Result if user simply clicks the close button top-right on dialog */
    public static final int NO_BUTTON_CLICKED_INDEX = -1;

    private static final Icon INFO_ICON_ICON;
    private static final Icon WARN_ICON_ICON;
    private static final Icon ERROR_ICON_ICON;
    private static final Icon QUESTION_ICON_ICON;

    private JFrame parent;
    private boolean neverAskAgainMode;
    private JCheckBox neverAskAgainCheckBox;
    private String title;
    private Icon icon;
    private JPanel innerPanel;
    private int initialSelection;
    private JButton[] buttons;
    private ValueModel buttonModel;
    private JDialog dialog;

    // Cache icons from the local system.
    static {
        INFO_ICON_ICON = UIManager.getIcon("OptionPane.informationIcon");
        WARN_ICON_ICON = UIManager.getIcon("OptionPane.warningIcon");
        ERROR_ICON_ICON = UIManager.getIcon("OptionPane.errorIcon");
        QUESTION_ICON_ICON = UIManager.getIcon("OptionPane.questionIcon");
    }

    /**
     * Generic dialog.
     * Flexible replacement for JOptionPane.
     * Should only be accessed via DialogFactory.
     *
     * @param parent
     * @param title
     * @param innerPanel
     * @param type
     * @param options
     * @param initialSelection
     * @param neverAskAgainText
     */
    public GenericDialog(JFrame parent,
                         String title,
                         JPanel innerPanel,
                         GenericDialogType type,
                         String[] options,
                         int initialSelection,
                         String neverAskAgainText) {

        this.parent = parent;
        this.title = title;
        this.innerPanel = innerPanel;
        this.initialSelection = initialSelection;

        validateArgs(options);

        initComponents(neverAskAgainText, type, options);
    }

    private void initComponents(String neverAskAgainText,
                                GenericDialogType type,
                                String[] options) {

        buttonModel = new ValueHolder(NO_BUTTON_CLICKED_INDEX);

        neverAskAgainMode = neverAskAgainText != null &&
                neverAskAgainText.trim().length() > 0;

        if (DEFAULT.equals(type)) {
            icon = null;
        } else if (INFO.equals(type)) {
            icon = INFO_ICON_ICON;
        } else if (WARN.equals(type)) {
            icon = WARN_ICON_ICON;
        } else if (ERROR.equals(type)) {
            icon = ERROR_ICON_ICON;
        } else if (QUESTION.equals(type)) {
            icon = QUESTION_ICON_ICON;
        } else {
            throw new IllegalArgumentException("Invalid type. Could not find icon for " + type);
        }

        buttons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            final int j = i;
            Action a = new AbstractAction(option) {
                public void actionPerformed(ActionEvent e) {
                    buttonModel.setValue(j);
                    dispose();
                }
            };
            JButton b = new JButton(a);
            buttons[i] = b;
        }

        if (neverAskAgainText != null) {
            neverAskAgainCheckBox = new JCheckBox(neverAskAgainText);
        }

    }

    public int display() {
        dialog = new JDialog(parent, title, true);

        FormLayout layout = new FormLayout("5dlu, pref, 5dlu, pref:grow, 5dlu",
                "5dlu, pref:grow, 5dlu, pref, 5dlu, pref, 5dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        if (icon != null) {
            builder.add(new JLabel(icon), cc.xy(2, 2, "center, top"));
        }
        builder.add(innerPanel, cc.xy(4, 2, "center, center"));

        if (neverAskAgainCheckBox != null) {
            builder.add(neverAskAgainCheckBox, cc.xywh(2, 4, 3, 1));
        }

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        int i = 0;
        for (JButton button : buttons) {
            bar.addRelatedGap();
            bar.addGridded(button);
            if (initialSelection == i++) {
                dialog.getRootPane().setDefaultButton(button);
            }
        }
        bar.addRelatedGap();
        builder.add(bar.getPanel(), cc.xywh(2, 6, 3, 1, "center, center"));

        dialog.getContentPane().add(builder.getPanel());
        dialog.getContentPane().setSize(innerPanel.getPreferredSize().width,
                innerPanel.getPreferredSize().height);
        dialog.pack();
        if (parent != null && parent.isVisible()) {
            int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
            dialog.setLocation(x, y);
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (int) (screenSize.getWidth() - dialog.getWidth()) / 2;
            int y = (int) (screenSize.getHeight() - dialog.getHeight()) / 2;
            dialog.setLocation(x, y);
        }

        dialog.setVisible(true);

        return (Integer) buttonModel.getValue();
    }

    public boolean isNeverAskAgain() throws IllegalStateException {
        if (!neverAskAgainMode) {
            throw new IllegalStateException(
                    "You cannot request the NeverAskAgain state " +
                            "for GenericDialogs that did not set " +
                            "neverAskAgainText in the constructor");
        }
        return neverAskAgainCheckBox.isSelected();
    }

    private void validateArgs(String[] options) {

        if (innerPanel == null) {
            throw new IllegalArgumentException("Expected a innerPanel to display");
        }

        if (title == null || title.trim().length() == 0) {
            throw new IllegalArgumentException("Expected a title to display");
        }

        if (options == null) {
            throw new IllegalArgumentException("Missing options");
        } else if (initialSelection < 0 || initialSelection >= options.length) {
            throw new IllegalArgumentException("Invalid initialSelection");
        }
    }

    private void dispose() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }
}
