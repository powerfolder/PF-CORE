package de.dal33t.powerfolder;

/**
 * Define the Password Policy for FileLinks
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public enum FileLinkPasswordPolicy {
    /**
     * No need to set a password.
     */
    OPTIONAL,
    /**
     * Show the link options to hint to enter a password.
     * Password may still be empty.
     */
    RECOMMENDED,
    /**
     * Show the link options and enforce to set a password.
     * If no password is set, don't allow to download the file via link.
     */
    REQUIRED;
}
