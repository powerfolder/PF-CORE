package de.dal33t.powerfolder.ui;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * TODO schaatser Add javadoc
 */
public class InvitationReceivedHandlerDefaultImpl extends PFComponent implements
    InvitationReceivedHandler
{
    public InvitationReceivedHandlerDefaultImpl(Controller controller) {
        super(controller);
    }
    public void invitationReceived(
        InvitationReceivedEvent invitationRecievedEvent)
    {
        final Invitation invitation = invitationRecievedEvent.getInvitation();
        final boolean processSilently = invitationRecievedEvent
            .isProcessSilently();
        final boolean forcePopup = invitationRecievedEvent.isProcessSilently();
        final FolderRepository repository = invitationRecievedEvent
            .getFolderRepository();
        if (invitation == null || invitation.folder == null) {
            throw new NullPointerException("Invitation/Folder is null");
        }
        if (getController().isUIOpen()) {
            Runnable worker = new Runnable() {
                public void run() {
                    // Check if already on folder
                    if (repository.hasJoinedFolder(invitation.folder)) {
                        // Already on folder, show message if not processing
                        // silently
                        if (!processSilently) {
                            // Popup application
                            getController().getUIController().getMainFrame()
                                .getUIComponent().setVisible(true);
                            getController().getUIController().getMainFrame()
                                .getUIComponent()
                                .setExtendedState(Frame.NORMAL);

                            getController().getUIController()
                                .showWarningMessage(
                                    Translation.getTranslation(
                                        "joinfolder.already_joined_title",
                                        invitation.folder.name),
                                    Translation.getTranslation(
                                        "joinfolder.already_joined_text",
                                        invitation.folder.name));
                        }
                        return;
                    }
                    final FolderJoinPanel panel = new FolderJoinPanel(
                        getController(), invitation, invitation.invitor);
                    final JFrame jFrame = getController().getUIController()
                        .getMainFrame().getUIComponent();
                    if (forcePopup
                        || !(OSUtil.isSystraySupported() && !jFrame.isVisible()))
                    {
                        // Popup whole application
                        getController().getUIController().getMainFrame()
                            .getUIComponent().setVisible(true);
                        getController().getUIController().getMainFrame()
                            .getUIComponent().setExtendedState(Frame.NORMAL);
                        open(panel);
                    } else {
                        // Only show systray blinking
                        getController().getUIController().getBlinkManager()
                            .setBlinkingTrayIcon(Icons.ST_INVITATION);
                        jFrame.addWindowFocusListener(new WindowFocusListener()
                        {
                            public void windowGainedFocus(WindowEvent e) {
                                jFrame.removeWindowFocusListener(this);
                                open(panel);
                            }

                            public void windowLostFocus(WindowEvent e) {
                            }
                        });
                    }
                }

                private void open(FolderJoinPanel panel) {
                    // Turn off blinking tray icon
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(null);
                    panel.open();
                    // Adding invitor to friends
                    if (panel.addInvitorToFriendsRequested()) {
                        Member node = invitation.invitor.getNode(
                            getController(), true);
                        // Set friend state
                        node.setFriend(true);
                    }

                    // Add folder to unjoin folder list if
                    // not joined and not secret
                    if (!repository.hasJoinedFolder(invitation.folder)) {
                        repository.addUnjoinedFolder(invitation.folder,
                            invitation.invitor.getNode(getController()));
                    }
                }
            };

            // Invoke later
            SwingUtilities.invokeLater(worker);
        }

    }
}
