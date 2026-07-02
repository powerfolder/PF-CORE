/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

import static de.dal33t.powerfolder.util.StringUtils.isNotBlank;

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

    public static void clear(NoticesModel model) {
        for (Notice notice: model.getAllNotices()) {
            if (notice instanceof CloudStorageNotice) {
                model.clearNotice(notice);
            }
        }
    }

    public Runnable getPayload(final Controller controller) {
        final String targetURI;
        String shopURL = ConfigurationEntry.PROVIDER_BUY_URL.getValue(controller);
        if (isNotBlank(shopURL)) {
            if (shopURL.toLowerCase().startsWith("http")) {
                return () -> BrowserLauncher.open(controller, () -> shopURL);
            }
            targetURI = shopURL;
        } else {
            targetURI = Constants.MY_ACCOUNT_URI;
        }
        return () -> BrowserLauncher.open(controller, () -> controller.getOSClient().getWebURL(
                targetURI, true));
    }
}
