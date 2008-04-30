package de.dal33t.powerfolder.transfer;


public class BrokenDownloadException extends Exception {
    private static final long serialVersionUID = 1L;
    private final TransferProblem problem;

    public BrokenDownloadException(TransferProblem problem,
        Exception e)
    {
        super(e);
        this.problem = problem;
    }

    public BrokenDownloadException() {
        this(null, null);
    }

    public BrokenDownloadException(Throwable cause) {
        super(cause);
        problem = TransferProblem.GENERAL_EXCEPTION;
    }

}
