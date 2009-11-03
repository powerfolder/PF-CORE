/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: CleanupTranslationFiles.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Message to force the client to reload the config from a given URL.
 * <p>
 * TRAC #1799
 * 
 * @author sprajc
 */
public class ConfigurationLoadRequest extends Message {
    private static final Logger LOG = Logger
        .getLogger(ConfigurationLoadRequest.class.getName());
    private static final long serialVersionUID = 1L;

    private String configURL;
    private boolean replaceExisting;
    private boolean restartRequired;
    // TRUE For bolloré. Remove after
    private boolean modifyWinINIConfigCentral = true;

    public ConfigurationLoadRequest(String configURL, boolean replaceExisting,
        boolean restartRequired, boolean modifyWinINIConfigCentral)
    {
        super();
        Reject.ifBlank(configURL, "Config URL");
        this.configURL = configURL;
        this.replaceExisting = replaceExisting;
        this.restartRequired = restartRequired;
        this.modifyWinINIConfigCentral = modifyWinINIConfigCentral;
    }

    public String getConfigURL() {
        return configURL;
    }

    public boolean isReplaceExisting() {
        return replaceExisting;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    public boolean isModifyWinINIConfigCentral() {
        return modifyWinINIConfigCentral;
    }

    public static void modifyWinINIConfigCentral() {
        File installPath = WinUtils.getProgramInstallationPath();
        if (installPath == null) {
            return;
        }
        File iniFile = new File(installPath, Constants.POWERFOLDER_INI_FILE);

        if (!iniFile.exists()) {
            LOG.warning("Unable to update ini file. Not found: " + iniFile);
            return;
        }
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            StreamUtils.copyToStream(iniFile, bOut);
            String iniContents = new String(bOut.toByteArray(), Convert.UTF8);
            String allConfigLine = "-D"
                + Feature.CONFIGURATION_ALL_USERS.getSystemPropertyKey()
                + "=true";
            if (iniContents.contains(allConfigLine)) {
                LOG
                    .fine("No need to update ini file. Already got central config setup");
                // No need to update
                return;
            }
            iniContents += "\r\n";
            iniContents += allConfigLine;
            FileUtils.copyFromStreamToFile(new ByteArrayInputStream(iniContents
                .getBytes(Convert.UTF8)), iniFile);
            LOG.info("Wrote new ini file: " + iniFile + ". Contents:\n"
                + iniContents);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to update ini file: " + iniFile
                + ". " + e, e);
        }
    }

    @Override
    public String toString() {
        return "ReloadConfig from " + configURL + ", replace existing? "
            + replaceExisting + ", restart? " + restartRequired;
    }

}
