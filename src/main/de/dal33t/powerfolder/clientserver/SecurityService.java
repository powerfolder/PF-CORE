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
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.Permission;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service for client authentication and permission checks.
 * <P>
 * TODO Traffic optimize
 *
 * @author sprajc
 */
public interface SecurityService {

    // Login stuff ************************************************************

    /**
     * Logs in from a remote location.
     *
     * @param username
     * @param password
     *            the password
     * @throws SecurityException
     *             if the log in failed, but credentials were right.
     * @return {@code True} if login succeeded, {@code false} if the credentials
     *         were wrong
     */
    boolean login(String username, char[] password);

    /**
     * Logs in from a remote location.
     *
     * @param username
     * @param credentials
     *            the credentials
     * @throws SecurityException
     *             if the log in failed, but credentials were right.
     * @return {@code True} if login succeeded, {@code false} if the credentials
     *         were wrong
     */
    boolean login(String username, byte[] credentials);

    /**
     * PFC-2548: Logs in by a token secret (device specific token).
     * 
     * @param tokenSecret
     * @return {@code True} if login succeeded, {@code false} if the token is
     *         invalid or token based authentication is disabled.
     */
    boolean login(String tokenSecret);

    /**
     * @return true if a user is logged in currently = has open session.
     */
    boolean isLoggedIn();

    /**
     * Logs out.
     */
    void logout();

    /**
     * Notifies nodes that their accounts have changed
     *
     * @param accounts The changed accounts
     */
    void notifyAccountStateChanged(Account... accounts);

    /**
     * PFS-862
     *
     * @return the valid OTP. Usable once within the next minute only.
     */
    String requestOTP();

    /**
     * PFC-2548
     * 
     * @return a generic device token to be used for further authentication. May
     *         have an expiration date.
     */
    String requestToken();

    /**
     * Request a number of tokens for an iOS device
     *
     * @return The requested tokens. Mapped nodeID -> tokenSecret
     */
    @Nullable Map<String, String> requestTokensIOS();

    /**
     * PFS-1685
     *
     * @return a generic token without any reference to a specific node
     */
    String requestWebDAVToken();

    // Nodes information retrieval ********************************************

    /**
     * @return Account details about the currently logged in user.
     */
    AccountDetails getAccountDetails();

    /**
     * Resulting map may not contain all nodes only those connected to the
     * server.
     *
     * @param nodes
     * @return the {@link AccountInfo} for the nodes.
     */
    Map<MemberInfo, AccountInfo> getAccountInfos(Collection<MemberInfo> nodes);

    /**
     * TRAC #1566
     *
     * @param pattern
     * @return the nodes
     */
    Collection<MemberInfo> searchNodes(String pattern);

    // Security / Permission stuff ********************************************

    /**
     * @param accountInfo
     * @param permission
     * @return true if the account with has that permission.
     */
    boolean hasPermission(AccountInfo accountInfo, Permission permission);

    /**
     * Bulk method to reduce RPC overhead. Supported by versions HIGHER than
     * "4.2.9".
     *
     * @param accountInfo
     * @param permissions
     * @return the list of results
     */
    List<Boolean> hasPermissions(AccountInfo accountInfo,
        List<Permission> permissions);

    /**
     * @param foInfo
     * @return the permissions on the folder.
     */
    Map<AccountInfo, FolderPermission> getFolderPermissions(FolderInfo foInfo);

    /**
     * @param foInfo
     * @return All permissions to an account and group on the folder.
     */
    Map<Serializable, FolderPermission> getAllFolderPermissions(FolderInfo foInfo);

    /**
     * Tries to obtain a permission on the given folder for the logged in
     * account.
     *
     * @param foInfo
     * @return the permission that was granted to the logged in account. null if
     *         not possible.
     */
    FolderPermission obtainFolderPermission(FolderInfo foInfo);

    /**
     * Changes a folder permission of a target account. Removes all existing
     * FolderPermissions of this account.
     *
     * @param aInfo
     *            the target account.
     * @param foInfo
     *            the folder
     * @param newPermission
     */
    void setFolderPermission(AccountInfo aInfo, FolderInfo foInfo,
        FolderPermission newPermission);

    /**
     * PFC-3543: How to seed the explicit permissions of a subfolder when its permission
     * inheritance is interrupted.
     */
    enum InheritanceInterruptMode {
        /**
         * Copy the currently inherited effective permissions as explicit permissions onto
         * the subfolder (default) - no user loses access immediately.
         */
        ADOPT_SNAPSHOT,
        /**
         * Start the subfolder without permissions, except the folder owner and the acting
         * admin (self-lockout protection).
         */
        DISCARD
    }

    /**
     * PFC-3543: Interrupts the permission inheritance of a subfolder. From now on only the
     * permissions set explicitly on the subfolder apply; changes on the parent no longer
     * affect it. The acting admin always keeps at least admin access (no self-lockout), and
     * the subfolder gets the same owner as its top folder. Applies the flag (version bump)
     * and switches the folder to its own database at runtime.
     * <p>
     * Must be invoked on the node hosting the folder - route via
     * {@code ServiceRegistry.getSecurityService(controller, subFolderInfo)}.
     *
     * @param subFolderInfo the subfolder whose inheritance to interrupt
     * @param mode          how to seed the subfolder's explicit permissions
     */
    void interruptInheritance(FolderInfo subFolderInfo, InheritanceInterruptMode mode);

    /**
     * PFC-3543: Restores the permission inheritance of a subfolder. The current explicit
     * permissions are archived, then removed, and the subfolder inherits from its parent
     * again (its database is merged back into the top folder).
     * <p>
     * Must be invoked on the node hosting the folder - route via
     * {@code ServiceRegistry.getSecurityService(controller, subFolderInfo)}.
     *
     * @param subFolderInfo the subfolder whose inheritance to restore
     */
    void restoreInheritance(FolderInfo subFolderInfo);

    /**
     * Returns all invitations for the logged in user
     *
     * @return the invitations
     */
    Collection<FolderPermission> getInvitations();

    /**
     * Returns all invitations to the folder
     *
     * @param folderInfo
     * @return the invitations on the folder.
     */
    Map<AccountInfo, FolderPermission> getInvitations(FolderInfo folderInfo);


    /**
     * Accept an invitation to a folder.
     *
     * @param invitation
     *          the invitation.
     */
    void acceptInvitation(Invitation invitation);

    /**
     * Decline an invitation to a folder.
     *
     * @param invitation
     *            the invitation.
     */
    void declineInvitation(Invitation invitation);

    /**
     * PF-102: In federation, get the hosting service of a given username.
     *
     * @param username The accounts username.
     * @return The ServerInfo the account is hosted on.
     */
    ServerInfo getHostingService(String username);

    /**
     * PFS-5630: Files a join request ("reverse invitation") of the logged in
     * user to a moderated folder. Stores the pending request and notifies the
     * folder managers by e-mail (deep-link to process the request).
     *
     * @param invitation
     *            the join request. Must have {@link Invitation#isJoinRequest()}
     *            set and the requester as recipient.
     */
    void requestJoin(Invitation invitation);

    /**
     * PFS-5630: Approves a pending join request to a moderated folder. Caller
     * must hold {@code FolderPermission.admin} on the folder. Grants the
     * permission carried by the invitation to the requester (if any - group
     * assignments are handled separately), deletes the request and notifies
     * the requester by e-mail including the manager comment.
     *
     * @param invitation
     *            the pending join request.
     * @param managerComment
     *            optional comment of the processing manager, may be null.
     * @param grantDirectPermission
     *            whether to grant the direct {@link FolderPermission} carried
     *            by the invitation to the requester.
     */
    void approveJoinRequest(Invitation invitation, String managerComment,
        boolean grantDirectPermission);

    /**
     * PFS-5630: Declines a pending join request to a moderated folder. Caller
     * must hold {@code FolderPermission.admin} on the folder. Deletes the
     * request (no grant) and notifies the requester by e-mail including the
     * manager comment.
     *
     * @param invitation
     *            the pending join request.
     * @param managerComment
     *            optional comment of the processing manager, may be null.
     */
    void declineJoinRequest(Invitation invitation, String managerComment);
}
