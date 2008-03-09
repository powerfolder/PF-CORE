package de.dal33t.powerfolder.ui.wizard;

/**
 * Names of attributes passed in a Wizard Context.
 */
public interface WizardContextAttributes {

    /** The attribute in wizard context, which will be displayed */
    String PROMPT_TEXT_ATTRIBUTE = "disklocation.prompt_text";

    /** The folder info object for the targeted folder */
    String FOLDERINFO_ATTRIBUTE = "disklocation.folder_info";

    /** The folder info object for the targeted folder */
    String SYNC_PROFILE_ATTRIBUTE = "disklocation.sync_profile";

    /** Determines if user should be prompted to send invitation afterwards */
    String SEND_INVIATION_AFTER_ATTRIBUTE = "disklocation.send_invitations";

    /** Determines if folder should be created as preview */
    String PREVIEW_FOLDER_ATTIRBUTE = "disklocation.preview_folder";

}
