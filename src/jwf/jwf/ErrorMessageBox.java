
package jwf;

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

/**
 * Displays a list of error messages and blocks until ok is pressed.
 * @author Christopher Brind
 */
public class ErrorMessageBox extends JDialog implements ActionListener {

    private final JTextPane textPane = new JTextPane();

    /** Construct a dialog with no parent. */
    public ErrorMessageBox() {
    }

    /** Construct a dialog with a frame parent. */
    public ErrorMessageBox(Frame frame) {
        super(frame, Translation.getTranslation("general.messages"), true);
        init();
        center(frame);
    }

    /** Construct a dialog with a dialog parent. */
    public ErrorMessageBox(Dialog dialog) {
        super(dialog, Translation.getTranslation("general.messages"), true);
        init();
        center(dialog);
    }

    private void center(Window window) {

        int x = window.getLocation().x +
                window.getSize().width / 2 - getSize().width / 2;
        int y = window.getLocation().y +
                window.getSize().height / 2 - getSize().height / 2;

        setLocation(x, y);
    }

    private void init() {
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textPane);
        container.add(scrollPane, BorderLayout.CENTER);
        UIUtil.removeBorder(scrollPane);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        JButton button = new JButton(Translation.getTranslation("general.ok") );
        button.addActionListener(this);
        getRootPane().setDefaultButton(button);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(button);
        container.add(buttonPanel, BorderLayout.SOUTH);
        textPane.setEditable(false);
        textPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        textPane.setOpaque(true);
        textPane.setBackground(SystemColor.text);
        setSize(300, 200);
    }

    /** Handles the ok press. */
    public void actionPerformed(ActionEvent e) {
        if (Translation.getTranslation("general.ok").equals(e.getActionCommand())) {
            setVisible(false);
        }
    }

    /** Show a list of messages and block until ok is pressed.
     * @param list a List of String objects.
     */
    public void showErrorMessages(List<String> list) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = list.iterator();
        while(iter.hasNext()) {
            String s = iter.next();
            builder.append(builder + s + (iter.hasNext() ? "\n" : ""));
        }
        textPane.setText(builder.toString());
        setVisible(true);
    }

}
