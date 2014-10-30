/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 *
 * $Id: ServerClient.java 14685 2010-12-21 14:16:43Z tot $
 */
package de.dal33t.powerfolder.util.update;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains settings for the updatecheck.
 */
public class UpdateSetting {
    public String versionCheckURL;
    /**
     * A info file containing the link that may override
     * <code>windowsExeURL</code> if existing.
     */
    public String downloadLinkInfoURL;
    public String windowsExeURL;

    /**
     * For JUnit tests only.
     */
    public UpdateSetting() {
    }

    /**
     * @param c
     * @return update settings to check and download new program client
     *         versions.
     */
    public static UpdateSetting create(Controller c) {
        Reject.ifNull(c, "Controller");
        UpdateSetting settings = new UpdateSetting();

        String webURL = "";
        if (c.getOSClient().hasWebURL()) {
            webURL = c.getOSClient().getWebURL();
        }

        settings.versionCheckURL = ConfigurationEntry.UPDATE_VERSION_URL
            .getValue(c);
        if (settings.versionCheckURL != null) {
            settings.versionCheckURL = settings.versionCheckURL.replace(
                "$server_url", webURL);
        }
        settings.downloadLinkInfoURL = ConfigurationEntry.UPDATE_DOWNLOADLINK_INFO_URL
            .getValue(c);
        if (settings.downloadLinkInfoURL != null) {
            settings.downloadLinkInfoURL = settings.downloadLinkInfoURL
                .replace("$server_url", webURL);
        }
        settings.windowsExeURL = ConfigurationEntry.UPDATE_WINDOWS_EXE_URL
            .getValue(c);
        if (settings.windowsExeURL != null) {
            settings.windowsExeURL = settings.windowsExeURL.replace(
                "$server_url", webURL);
        }
        return settings;
    }

}