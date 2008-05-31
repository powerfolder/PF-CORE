package de.dal33t.powerfolder.ui.wizard;

/**
 * Names of attributes passed in a Wizard Context.
 */
public interface WizardContextAttributes {

    /** The attribute in wizard context, which will be displayed */
    String PROMPT_TEXT_ATTRIBUTE = "disklocation.prompt_text";

    /** The folder info object for the targeted folder */
    String FOLDERINFO_ATTRIBUTE = "disklocation.folder_info";

    /** The local file location for the folder */
    String FOLDER_LOCAL_BASE = "disklocation.localbase";

    /** The folder info object for the targeted folder */
    String SYNC_PROFILE_ATTRIBUTE = "disklocation.sync_profile";

    /** Determines if user should be prompted to send invitation afterwards */
    String SEND_INVIATION_AFTER_ATTRIBUTE = "disklocation.send_invitations";
    
    /** If a desktop shortcut should be created */
    String CREATE_DESKTOP_SHORTCUT = "foldercreate.desktop_shortcut";
    
    /** If the folder should be backed up by the Online Storage */
    String BACKUP_ONLINE_STOARGE = "foldercreate.backup_by_os";

    /** Determines if folder should be created as preview */
    String PREVIEW_FOLDER_ATTIRBUTE = "disklocation.preview_folder";

    /** Determines if in basic setup */
    String BASIC_SETUP_ATTIRBUTE = "basic_setup";

}
