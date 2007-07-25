package de.dal33t.powerfolder.transfer;

/**
 * Enum of variaous problems that can occur when transfering files.
 */
public enum TransferProblem {

    TRANSFER_BROKEN("transfer.problem.transfer.broken"),
    OLD_UPLOAD("transfer.problem.old.upload"),
    BROKEN_DOWNLOAD("transfer.problem.broken.download"),
    BROKEN_UPLOAD("transfer.problem.broken.upload"),
    TRANSFER_EXCEPTION("transfer.problem.transfer.exception"),
    INVALID_PART("transfer.problem.invalid.part"),
    FILE_NOT_FOUND_EXCEPTION("transfer.problem.file.not.found.exception"),
    IO_EXCEPTION("transfer.problem.io.exception"),
    NO_SUCH_ALGORITHM_EXCEPTION("transfer.problem.no.such.algorithm.exception"),
    TEMP_FILE_DELETE("transfer.problem.temp.file.delete"),
    TEMP_FILE_OPEN("transfer.problem.temp.file.open"),
    TEMP_FILE_WRITE("transfer.problem.temp.file.write"),
    ILLEGAL_CHUNK("transfer.problem.illegal.chunk"),
    MD5_ERROR("transfer.problem.md5.error"),
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
