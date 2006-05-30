package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;

/**
 * use DialogFactory to display this.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class ScrollableOkCancelDialog extends BaseDialog {
    private int choice = JOptionPane.CANCEL_OPTION;
    private String title;
    private String message;
    private String longText;
    private Icon icon;

    public ScrollableOkCancelDialog(Controller controller, boolean modal,
        boolean border, String title, String message, String longText, Icon icon)
    {
        super(controller, modal, border);
        this.title = title;
        this.message = message;
        this.longText = longText;
        this.icon = icon;
    }

    /**
     * @return the choice of the user, this is JOptionPane competible, either
     *         JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION.
     */
    public int getChoice() {
        return choice;
    }

    @Override
    protected Component getButtonBar()
    {
        JButton okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                choice = JOptionPane.OK_OPTION;
                setVisible(false);
            }
        });
        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                choice = JOptionPane.CANCEL_OPTION;
                setVisible(false);
            }
        });
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    @Override
    protected Component getContent()
    {
        JTextArea textArea = new JTextArea(longText, 10, 30);
        textArea.setBackground(Color.WHITE);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        FormLayout layout = new FormLayout("pref", "pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(message).getPanel(), cc.xy(1, 1));
        builder.add(scrollPane, cc.xy(1, 2));
        return builder.getPanel();
    }

    @Override
    protected Icon getIcon()
    {
        return icon;
    }

    @Override
    public String getTitle()
    {
        return title;
    }
}
