package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

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

    public NeverAskAgainMessageDialog(Frame parent, String title,
        String message, String showAgainText)
    {
        super(parent, title, true); // true = modal
        this.message = message;
        this.showAgainText = showAgainText;
        initComponents();
    }

    private void initComponents() {
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
        JButton okButton;
        JComponent buttonBar;

        okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
        okButton.setMnemonic(Translation.getTranslation("general.ok.key").trim().charAt(0));        
        buttonBar = ButtonBarFactory.buildCenteredBar(okButton);

        return buttonBar;
    }

}
