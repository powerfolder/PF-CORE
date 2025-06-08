package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.util.Translation;

public class VersionTooOldNotice extends WarningNotice {
    private static final long serialVersionUID = 100L;

    public VersionTooOldNotice() {
        super(Translation.get("warning_notice.title"), Translation.get("warning_notice.version_too_old_summary"),
                Translation.get("warning_notice.version_too_old_message"));
    }
}
