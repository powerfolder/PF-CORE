package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

public class CloudStorageNotice extends WarningNotice {
    private static final long serialVersionUID = 100L;

    public static CloudStorageNotice almostFull() {
        return new CloudStorageNotice(Translation.get("warning_notice.cloud_almost_full_summary"));
    }
    public static CloudStorageNotice full() {
        return new CloudStorageNotice(Translation.get("warning_notice.cloud_full_summary"));
    }
    private CloudStorageNotice(String summary) {
        super(Translation.get("warning_notice.title"),  summary, null);
    }

    public Runnable getPayload(final Controller controller) {
        return () -> BrowserLauncher.open(controller, () -> controller.getOSClient().getWebURL(
                Constants.MY_ACCOUNT_URI, true));
    }
}
