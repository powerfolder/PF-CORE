package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Translation;

/**
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class NeverAskAgainMessageDialog extends JDialog {

    private String message;
    private JCheckBox checkBox;
    private String showAgainText;
    private String[] optionTexts;
    private int buttonIndex = -1;

    public NeverAskAgainMessageDialog(Frame parent, String title,
                                      String message, String showAgainText,
                                      String[] optionTexts) {
        super(parent, title, true); // true = modal
        this.message = message;
        this.showAgainText = showAgainText;
        this.optionTexts = optionTexts;
        initComponents();
    }

    private void initComponents() {

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        checkBox = new JCheckBox(showAgainText);

        FormLayout layout = new FormLayout("pref",
                "pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(getTextPanel(), cc.xy(1, 1));
        builder.add(checkBox, cc.xy(1, 3));
        builder.add(getButtonBar(), cc.xy(1, 5));

        builder.setBorder(Borders
                .createEmptyBorder("10dlu, 10dlu, 10dlu, 10dlu"));

        JPanel panel = builder.getPanel();
        getContentPane().add(panel);
        getContentPane().setSize(panel.getPreferredSize().width,
                panel.getPreferredSize().height);
        pack();

        Component parent = getOwner();
        int x = parent.getX() + (parent.getWidth() - getWidth()) / 2;
        int y = parent.getY() + (parent.getHeight() - getHeight()) / 2;
        setLocation(x, y);
    }

    private JPanel getTextPanel() {
        PanelBuilder builder = LinkedTextBuilder.build(message);
        builder.setBorder(Borders.createEmptyBorder("0, 10dlu, 10dlu, 10dlu"));
        return builder.getPanel();
    }

    public boolean showNeverAgain() {
        return checkBox.isSelected();
    }

    protected Component getButtonBar() {

        JButton[] buttons = new JButton[optionTexts.length];
        int i = 0;
        for (String optionText : optionTexts) {
            JButton b = new JButton(optionText);
            final int j = i;
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    buttonIndex = j;
                    dispose();
                }
            });
            buttons[i++] = b;
        }

        return ButtonBarFactory.buildCenteredBar(buttons);
    }

    public NeverAskAgainResponse getResponse() {
        return new NeverAskAgainResponse(buttonIndex,
                checkBox.isSelected());
    }
}
