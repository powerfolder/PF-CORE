package de.dal33t.powerfolder.transfer;

/**
 * Enum of variaous problems that can occur when transfering files.
 */
public enum TransferProblem {

    NODE_DISCONNECTED("transfer.problem.transfer.broken"),
    OLD_UPLOAD("transfer.problem.old.upload"),
    BROKEN_DOWNLOAD("transfer.problem.broken.download"),
    BROKEN_UPLOAD("transfer.problem.broken.upload"),
    /**
     * This is a canidate to be replace through other problem codes.
     */
    TRANSFER_EXCEPTION("transfer.problem.transfer.exception"),
    INVALID_PART("transfer.problem.invalid.part"),
    FILE_NOT_FOUND_EXCEPTION("transfer.problem.file.not.found.exception"),
    IO_EXCEPTION("transfer.problem.io.exception"),
    TEMP_FILE_DELETE("transfer.problem.temp.file.delete"),
    TEMP_FILE_OPEN("transfer.problem.temp.file.open"),
    TEMP_FILE_WRITE("transfer.problem.temp.file.write"),
    ILLEGAL_CHUNK("transfer.problem.illegal.chunk"),
    MD5_ERROR("transfer.problem.md5.error"),
    /**
     * This one is too general.
     */
    GENERAL_EXCEPTION("transfer.problem.general.exception");

    /** Translation id that should be in the Translation.properties file */
    private String translationId;

    /**
     * Constructor
     *
     * @param translationId
     *         the translation id.
     */
    TransferProblem(String translationId) {
        this.translationId = translationId;
    }

    /**
     * Gets the trnslation id.
     * @return
     */
    public String getTranslationId() {
        return translationId;
    }
}
