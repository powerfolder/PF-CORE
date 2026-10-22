/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.dal33t.powerfolder.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * PFC-3301: Shows the open (pending) folder invitations as the first entries of
 * the folder list - like the web interface. Each invitation shows the folder
 * name and, under a directions icon + caret, an accept / decline dropdown.
 * Clicking the name accepts; declining asks for confirmation.
 * <p>
 * On login the account's own pending invitations are pulled from the server so
 * they also appear after a restart (not only the ones received live this
 * session).
 */
public class InvitationsPanel extends PFUIComponent {

    /** Folder names longer than this are shown truncated with a trailing "...". */
    private static final int MAX_NAME_LENGTH = 40;

    private JPanel uiComponent;
    private JPanel listPanel;

    public InvitationsPanel(Controller controller) {
        super(controller);
    }

    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initComponents();
            rebuild();
        }
        return uiComponent;
    }

    private void initComponents() {
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        uiComponent = new JPanel(new BorderLayout());
        // No left inset here - each row carries the same 6dlu left inset as the
        // folder rows, so the icon/text line up with the folder list below.
        uiComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        uiComponent.add(listPanel, BorderLayout.CENTER);
        uiComponent.setVisible(false);

        // Rebuild whenever the open invitations change.
        PropertyChangeListener rebuildListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(() -> rebuild());
            }
        };
        getNoticesModel().getUnreadInvitationsCountVM().addValueChangeListener(rebuildListener);

        // Pull the account's own pending invitations from the server on every
        // login, plus once now in case auto-login already happened before this
        // panel was built.
        getController().getOSClient().addListener(new MyLoginListener());
        if (getController().getOSClient().isLoggedIn()) {
            fetchOpenInvitations();
        }
    }

    private NoticesModel getNoticesModel() {
        return getUIController().getApplicationModel().getNoticesModel();
    }

    /**
     * Fetches the account's own pending invitations (full invitations incl. OID,
     * so accept/decline work) and feeds them into the normal invitation handling,
     * which turns them into notices shown by this panel. Invitations already known
     * as a notice are skipped, so a reconnect does not pop them up again.
     */
    private void fetchOpenInvitations() {
        getController().getThreadPool().execute(() -> {
            ServerClient client = getController().getOSClient();
            AccountInfo accountInfo = client.getAccountInfo();
            if (!client.isLoggedIn() || accountInfo == null) {
                return;
            }
            try {
                Collection<Invitation> invitations = client.getAccountService().getInvitationsFor(accountInfo);
                if (invitations == null) {
                    return;
                }
                logFine(accountInfo + ": " + invitations.size() + " open invitation(s) fetched");
                for (Invitation invitation : invitations) {
                    if (!isKnown(invitation)) {
                        getController().invitationReceived(invitation);
                    }
                }
            } catch (Exception e) {
                logWarning(accountInfo + ": Open invitations not fetched: " + e);
            }
        });
    }

    private boolean isKnown(Invitation invitation) {
        for (Notice notice : getNoticesModel().getAllNotices()) {
            if (notice instanceof InvitationNotice) {
                Invitation known = ((InvitationNotice) notice).getPayload(getController());
                if (known != null && known.getOID() != null && known.getOID().equals(invitation.getOID())) {
                    return true;
                }
            }
        }
        return false;
    }

    private class MyLoginListener implements ServerClientListener {
        public boolean fireInEventDispatchThread() {
            return false;
        }

        public void login(ServerClientEvent event) {
            fetchOpenInvitations();
        }

        public void accountUpdated(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
        }

        public void childClientSpawned(ServerClientEvent event) {
        }
    }

    /**
     * @return true if the current theme uses a dark panel background, so the
     *         white icon variants should be used for contrast.
     */
    private boolean isDarkBackground() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) {
            bg = UIManager.getColor("control");
        }
        return bg != null
            && (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) < 128;
    }

    /**
     * Picks the icon variant that contrasts with the current theme.
     *
     * @param lightBgId icon id for a light background (black glyph)
     * @param darkBgId  icon id for a dark background (white glyph)
     */
    private Icon themedIcon(String lightBgId, String darkBgId) {
        return Icons.getIconById(isDarkBackground() ? darkBgId : lightBgId);
    }

    /**
     * Rebuilds the list of open invitation rows. Hides the panel when there are
     * none.
     */
    private void rebuild() {
        listPanel.removeAll();

        int shown = 0;
        for (Notice notice : getNoticesModel().getAllNotices()) {
            if (notice.isRead() || !(notice instanceof InvitationNotice)) {
                continue;
            }
            Invitation invitation = ((InvitationNotice) notice)
                .getPayload(getController());
            if (invitation == null) {
                continue;
            }
            FolderInfo folder = invitation.folder;
            // Skip invitations to folders we already joined.
            if (folder != null
                && getController().getFolderRepository().hasJoinedFolder(folder))
            {
                continue;
            }
            listPanel.add(createRow(notice, invitation, folder));
            shown++;
        }

        uiComponent.setVisible(shown > 0);
        uiComponent.revalidate();
        uiComponent.repaint();
        // The panel lives inside a BoxLayout/scroll pane; re-layout the parent so
        // a visibility change (e.g. an invitation loaded after startup) shows up.
        Container parent = uiComponent.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private JComponent createRow(final Notice notice, final Invitation invitation,
        final FolderInfo folder)
    {
        String folderName = folder != null ? folder.getLocalizedName() : "";
        String sender = invitation.getSender();
        // Shorten long folder names with a trailing ellipsis; the full name
        // stays available as the tooltip.
        String displayName = folderName.length() > MAX_NAME_LENGTH
            ? folderName.substring(0, MAX_NAME_LENGTH) + "..." : folderName;

        // Columns mirror the folder rows (ExpandableFolderView): a 6dlu left
        // inset, then the icon, the name (grow) and the actions. So the paper
        // plane lines up with the folder icons and the text with folder names.
        FormLayout layout = new FormLayout(
            "3dlu, 3dlu, pref, 3dlu, pref:grow, 3dlu, pref, 3dlu",
            "3dlu, pref, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Paper plane in its own column, aligned with the folder icons (24x24).
        JLabel iconLabel = new JLabel(themedIcon(Icons.OPEN_INVITATIONS,
            Icons.OPEN_INVITATIONS_WHITE));
        builder.add(iconLabel, cc.xy(3, 2));

        // Clicking the invitation name accepts it.
        ActionLabel folderLabel = new ActionLabel(getController(),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    acceptInvitation(notice, invitation);
                }
            });
        folderLabel.setNeverUnderline(true);
        folderLabel.setText(Translation.get(
            "main_frame.invitation.row.text", displayName));
        folderLabel.setToolTipText(sender != null
            ? Translation.get("notice.invitation.summary", sender, folderName) : folderName);
        builder.add(folderLabel.getUIComponent(), cc.xy(5, 2));

        // Accept / decline live in a dropdown under the directions icon + caret,
        // like the web.
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem acceptItem = new JMenuItem(Translation.get("general.accept"),
            themedIcon(Icons.INVITATION_ACCEPT, Icons.INVITATION_ACCEPT_WHITE));
        acceptItem.addActionListener(e -> acceptInvitation(notice, invitation));
        JMenuItem declineItem = new JMenuItem(Translation.get("general.decline"),
            themedIcon(Icons.INVITATION_DECLINE, Icons.INVITATION_DECLINE_WHITE));
        declineItem.addActionListener(e -> decline(notice, invitation, folderName));
        menu.add(acceptItem);
        menu.addSeparator();
        menu.add(declineItem);

        final JButton manageButton = new JButton(themedIcon(
            Icons.INVITATION_MANAGE, Icons.INVITATION_MANAGE_WHITE));
        // Down caret (U+25BE) after the icon so it reads as a dropdown.
        manageButton.setText(new String(Character.toChars(0x25BE)));
        manageButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        manageButton.setIconTextGap(2);
        manageButton.setToolTipText(Translation.get("general.accept") + " / "
            + Translation.get("general.decline"));
        manageButton.setBorderPainted(false);
        manageButton.setContentAreaFilled(false);
        manageButton.setFocusPainted(false);
        manageButton.addActionListener(
            e -> menu.show(manageButton, 0, manageButton.getHeight()));
        builder.add(manageButton, cc.xy(7, 2));

        JPanel row = builder.getPanel();
        row.setOpaque(false);
        return row;
    }

    private void acceptInvitation(final Notice notice, final Invitation invitation) {
        answer(notice, invitation, () -> getController().getOSClient().getSecurityService()
            .acceptInvitation(invitation));
    }

    /**
     * Sends accept or decline to the server. Only on success the invitation is
     * marked read (and its row disappears); a failure keeps the row and tells the
     * user.
     */
    private void answer(final Notice notice, final Invitation invitation, final Runnable serverCall) {
        getController().getThreadPool().execute(() -> {
            try {
                serverCall.run();
                SwingUtilities.invokeLater(() -> getNoticesModel().markRead(notice));
            } catch (RuntimeException e) {
                logWarning(invitation.folder + ": Invitation not answered: " + e);
                String folderName = invitation.folder != null ? invitation.folder.getLocalizedName() : "";
                SwingUtilities.invokeLater(() -> DialogFactory.genericDialog(getController(),
                    Translation.get("notice.invitation.title"),
                    Translation.get("main_frame.invitation.failed.text", folderName), GenericDialogType.ERROR));
            }
        });
    }

    private void decline(final Notice notice, final Invitation invitation,
        String folderName)
    {
        int response = DialogFactory.genericDialog(getController(),
            Translation.get("main_frame.invitation.decline.title"),
            Translation.get("main_frame.invitation.decline.text", folderName),
            new String[]{Translation.get("general.yes"),
                Translation.get("general.no")},
            1, GenericDialogType.DEFAULT);
        if (response == 0) {
            // Same as the web interface: decline the invitation server-side.
            answer(notice, invitation, () -> getController().getOSClient().getSecurityService()
                .declineInvitation(invitation));
        }
    }
}
