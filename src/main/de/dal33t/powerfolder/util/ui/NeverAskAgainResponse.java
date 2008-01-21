package de.dal33t.powerfolder.util.ui;

/**
 * Response from a NeverAskAgain dialog.
 */
public class NeverAskAgainResponse {

    private final boolean neverAskAgain;
    private final int buttonIndex;

    /**
     * Response from a NeverAskAgain dialog.
     *
     * @param buttonIndex 0 = first button, 1 = second, etc.
     * -1 if dialog cancelled.
     * @param neverAskAgain true if never askagain checked.
     */
    public NeverAskAgainResponse(int buttonIndex, boolean neverAskAgain) {
        this.buttonIndex = buttonIndex;
        this.neverAskAgain = neverAskAgain;
    }

    /**
     * Selected index bitton.
     * 0 = first button, 1 = second, etc.
     * -1 if dialog cancelled.
     *
     * @return button index.
     */
    public int getButtonIndex() {
        return buttonIndex;
    }

    /**
     * If never show again.
     *
     * @return true if never askagain checked.
     */
    public boolean isNeverAskAgain() {
        return neverAskAgain;
    }
}
