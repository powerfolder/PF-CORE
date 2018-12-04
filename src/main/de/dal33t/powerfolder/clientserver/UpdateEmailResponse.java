package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.security.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static de.dal33t.powerfolder.clientserver.AccountService.TOKEN_TYPE_ADD_EMAIL;
import static de.dal33t.powerfolder.clientserver.AccountService.TOKEN_TYPE_MERGE;

/**
 * Status and information about updating Emails of an {@link Account}
 *
 * {@code StatusCode StatusCodes} have a special meaning:
 * <table>
 *     <thead>
 *         <tr>
 *             <td>StatusCode</td>
 *             <td>Meaning</td>
 *             <td>Additional Data</td>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>OK(200)</td>
 *             <td>Only removed Emails</td>
 *             <td>A list of removed Email addresses</td>
 *         </tr>
 *         <tr>
 *             <td>ACCEPTED(202)</td>
 *             <td>Merge or Email verification needed (see the type)</td>
 *             <td>An Email address of the account to merge or a list of Email addresses to be verified and a type indicating merge or add Email</td>
 *         </tr>
 *         <tr>
 *             <td>NO_CONTENT(204)</td>
 *             <td>Nothing changed</td>
 *             <td>No additional data</td>
 *         </tr>
 *         <tr>
 *             <td>FORBIDDEN(403)</td>
 *             <td>Merge not allowed on this server</td>
 *             <td>An Email address and a type</td>
 *         </tr>
 *     </tbody>
 * </table>
 */
public class UpdateEmailResponse {
    @NotNull
    private final StatusCode  status;
    @Nullable
    private final Set<String> emails;
    @Nullable
    private final String      type;


    // Creation ---
    private UpdateEmailResponse(
        @NotNull StatusCode status)
    {
        this.status = status;
        this.emails = null;
        this.type   = null;
    }

    private UpdateEmailResponse(
        @NotNull  StatusCode status,
        @NotNull  String     email,
        @Nullable String     type)
    {
        this.status = status;
        this.emails = new HashSet<>(1);
        this.emails.add(email);
        this.type   = type;
    }

    private UpdateEmailResponse(
        @NotNull  StatusCode  status,
        @NotNull  Set<String> emails,
        @Nullable String      type)
    {
        this.status = status;
        this.emails = emails;
        this.type   = type;
    }

    /**
     * Create an {@link UpdateEmailResponse} with {@link StatusCode#OK}
     *
     * @param emails A set of email addresses that were removed from the account.
     * @return {@code UpdateEmailResponse} indicating that Emails were removed. May
     * contain a list of removed addresses.
     */
    public static UpdateEmailResponse createRemovedEmails(@NotNull Set<String> emails) {
        return new UpdateEmailResponse(StatusCode.OK, emails, null);
    }

    /**
     * Create an {@link UpdateEmailResponse} with {@link StatusCode#NO_CONTENT}
     *
     * @return {@code UpdateEmailResponse} indicating that nothing changed. Does not
     * contain any further information.
     */
    public static UpdateEmailResponse createNothingChanged() {
        return new UpdateEmailResponse(StatusCode.NO_CONTENT);
    }

    /**
     * Create an {@link UpdateEmailResponse} with {@link StatusCode#FORBIDDEN}
     *
     * @param email An email address that was not allowed to be merged or added.
     * @param type The type of operation that was tried. Either {@code 'merge'} or {@code 'addEmail‘}.
     * @return {@code UpdateEmailResponse} indicating that the operation was not
     * allowed. Does not contain any further information.
     */
    public static UpdateEmailResponse createNotAllowed(@NotNull String email, @NotNull String type) {
        return new UpdateEmailResponse(StatusCode.FORBIDDEN, email, type);
    }

    /**
     * Create an {@link UpdateEmailResponse} with {@link StatusCode#CONTINUE}
     *
     * @param emails A set of email addresses that need to be verified.
     * @return {@code UpdateEmailResponse} indicating that an Email was sent to
     * those Email addresses. The user has to verify that he/she has access
     * to those Email accounts. Contains a list of all affected Emails.
     */
    public static UpdateEmailResponse createEmailVerificationNeeded(@NotNull Set<String> emails) {
        return new UpdateEmailResponse(StatusCode.ACCEPTED, emails, TOKEN_TYPE_MERGE);
    }

    /**
     * Create an {@link UpdateEmailResponse} with {@link StatusCode#PROCESSING}
     *
     * @param email An email address of an account that needs to be verified to be merged.
     * @return {@code UpdateEmailResponse} indicating that the user has to verify to
     * merge two accounts. Contains the Email of the account to merge.
     */
    public static UpdateEmailResponse createMergeVerificationNeeded(@NotNull String email) {
        return new UpdateEmailResponse(StatusCode.ACCEPTED, email, TOKEN_TYPE_ADD_EMAIL);
    }
    // ---

    // Access ---
    @NotNull
    public StatusCode getStatus() {
        return status;
    }

    @Nullable
    public Set<String> getEmails() {
        return emails;
    }

    @Nullable
    public String getType() {
        return type;
    }
    // ---
}