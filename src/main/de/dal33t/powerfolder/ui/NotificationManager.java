package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.LineBorder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.chat.MemberChatPanel;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelEvent;
import de.dal33t.powerfolder.ui.chat.ChatModel.ChatModelListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.Slider;

/**
 * Listens to the chat model events and brings up the animated sliding notification view.
 *  
 * @author <A HREF="mailto:magapov@gmail.com">Maxim Agapov</A>
 * @version $Revision: 1.1 $
 */
public class NotificationManager extends PFUIComponent {
    
    /**
     * create a NotificationManager. 
     * 
     * @param controller
     *            the controller
     * @param chatModel
     *            the chat model to act upon
     */
    public NotificationManager(Controller controller) {
        super(controller);
        ChatModel chatModel = getUIController().getChatModel();
        if (chatModel == null) {
            throw new IllegalStateException("NotificationManager: chatModel is null");
        }
        chatModel.addChatModelListener(new MyChatModelListener());
    }

    // Internal classes ********************************************************

    /*
     * ChatModel event listener. Uses ChatNotificationView to display message.  
     */
    private class MyChatModelListener implements ChatModelListener {
        public void chatChanged(ChatModelEvent event) {
            if (event.isStatus()) {
                // Ignore status updates
                return;
            }

            if (event.getSource() instanceof Member) {
                boolean notInMemberChatPanel = !(getUIController().getInformationQuarter()
                                    .getDisplayTarget() instanceof MemberChatPanel);
                //display message if main window is minimized or not in the chat panel
                if (getUIController().getMainFrame().isIconified() || notInMemberChatPanel)
                {
                    new ChatNotificationView(getController(), ((Member) event.getSource()), event.getMessage()).show();
                }
            }
        }
    }
    
    /*
     * Displays animated new chat message box with message test, Accept and Close
     * buttons.
     * 
     * @see <code>Slider</code>
     */
    private class ChatNotificationView extends PFComponent implements ActionListener
    {

        public static final String OPTION1 = "Answer";
        public static final String OPTION2 = "Close";

        private Slider slider;
        private Member member;
        private String message;

        /**
         * Initializes new message box
         * 
         * @param controller
         * @param chatMessageMember
         */
        public ChatNotificationView(Controller controller,
            Member chatMessageMember, String message)
        {
            super(controller);
            Validate.notNull(chatMessageMember,
                "ChatMessageMember must not be null");
            Validate.notNull(message, "Message must not be null");
            this.member = chatMessageMember;
            this.message = message;

        }

        /**
         * Show the message using Slider
         * 
         * @param message
         */
        public void show() {
            if (slider == null) {
                JComponent contentPane = createNotificationForm(member.getNick(),
                    message);
                slider = new Slider(contentPane);
                slider.show();
            }
        }

        /*
         * Create notification form and extract contentPane
         */
        private JComponent createNotificationForm(String nick, String message) {
            JWindow dialog = new JWindow();
            Container contentPane = dialog.getContentPane();
            contentPane.setLayout(new BorderLayout());
            contentPane.add(new ChatNotificationForm(StringEscapeUtils
                .escapeHtml(nick), StringEscapeUtils.escapeHtml(message), this),
                BorderLayout.CENTER);
            dialog.pack();
            return (JComponent) contentPane;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(ActionEvent e) {
            if (slider != null) {
                slider.close();
                slider = null;
                if (OPTION1.equals(e.getActionCommand())) {
                    switchToMemberChat();
                }
            }
        }

        /*
         * Switch to chat panel 
         */
        private void switchToMemberChat() {
            getUIController().getControlQuarter().setSelected(member);
            MainFrame mf = getUIController().getMainFrame();
            if (mf.isIconified()) {
                mf.deiconify();
            }
        }

        /*
         * Inner class representing the message notification form
         */
        private class ChatNotificationForm extends JPanel {

            private static final String CHAT_NOTIFICATION_SUBTITLE = "chat.notification.subtitle";
            private static final String CHAT_NOTIFICATION_OPTION2 = "chat.notification.option2";
            private static final String CHAT_NOTIFICATION_OPTION1 = "chat.notification.option1";
            private static final long serialVersionUID = 1L;

            /**
             * Default constructor
             */
            public ChatNotificationForm(String nick, String msgText,
                ActionListener listener)
            {
                setLayout(new BorderLayout());
                add(createPanel(nick, msgText, listener), BorderLayout.CENTER);
                setBorder(new LineBorder(Color.black, 1));
            }

            /*
             * Adds fill components to empty cells in the first row and first column
             * of the grid. This ensures that the grid spacing will be the same as
             * shown in the designer.
             * 
             * @param cols
             *            an array of column indices in the first row where fill
             *            components should be added.
             * @param rows
             *            an array of row indices in the first column where fill
             *            components should be added.
             */
            private void addFillComponents(Container panel, int[] cols, int[] rows)
            {
                Dimension filler = new Dimension(10, 10);

                boolean filled_cell_11 = false;
                CellConstraints cc = new CellConstraints();
                if (cols.length > 0 && rows.length > 0) {
                    if (cols[0] == 1 && rows[0] == 1) {
                        /** add a rigid area */
                        panel.add(Box.createRigidArea(filler), cc.xy(1, 1));
                        filled_cell_11 = true;
                    }
                }

                for (int index = 0; index < cols.length; index++) {
                    if (cols[index] == 1 && filled_cell_11) {
                        continue;
                    }
                    panel.add(Box.createRigidArea(filler), cc.xy(cols[index], 1));
                }

                for (int index = 0; index < rows.length; index++) {
                    if (rows[index] == 1 && filled_cell_11) {
                        continue;
                    }
                    panel.add(Box.createRigidArea(filler), cc.xy(1, rows[index]));
                }

            }

            /*
             * Create the UI for notification form
             */
            private JPanel createPanel(String nick, String msgText,
                ActionListener listener)
            {
                JPanel jpanel1 = new JPanel();
                FormLayout formlayout1 = new FormLayout(
                    "FILL:10PX:NONE,FILL:73PX:NONE,FILL:10PX:NONE,FILL:73PX:NONE,FILL:10PX:NONE",
                    "CENTER:40PX:NONE,CENTER:90PX:NONE,CENTER:10PX:NONE,CENTER:DEFAULT:NONE,CENTER:DEFAULT:NONE");
                CellConstraints cc = new CellConstraints();
                jpanel1.setLayout(formlayout1);

                JButton jbutton1 = new JButton();
                jbutton1.setActionCommand(OPTION1);
                String option1 = Translation.getTranslation(CHAT_NOTIFICATION_OPTION1);
                jbutton1.setText(option1);
                jbutton1.addActionListener(listener);
                jpanel1.add(jbutton1, cc.xy(2, 4));

                JButton jbutton2 = new JButton();
                jbutton2.setActionCommand(OPTION2);
                String option2 = Translation.getTranslation(CHAT_NOTIFICATION_OPTION2);
                jbutton2.setText(option2);
                jbutton2.addActionListener(listener);
                jpanel1.add(jbutton2, cc.xy(4, 4));

                JLabel jlabel1 = new JLabel();
                String title = Translation.getTranslation(CHAT_NOTIFICATION_SUBTITLE);
                jlabel1.setText(title);
                jlabel1.setHorizontalAlignment(JLabel.CENTER);
                jpanel1.add(jlabel1, new CellConstraints(1, 1, 5, 1,
                        CellConstraints.CENTER, CellConstraints.TOP));

                JLabel jlabel2 = new JLabel();
                jlabel2.setText("<html><b>" + nick + ":</b> " + msgText + "</html>");
                jpanel1.add(jlabel2, new CellConstraints(2, 2, 3, 1,
                    CellConstraints.DEFAULT, CellConstraints.TOP));

                addFillComponents(jpanel1, new int[]{2, 3, 4, 5}, new int[]{2, 3,
                    4, 5});
                return jpanel1;
            }
        }
    }
}
