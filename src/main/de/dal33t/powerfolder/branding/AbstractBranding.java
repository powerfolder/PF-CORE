/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: ServerClient.java 6435 2009-01-21 23:35:04Z tot $
 */
package de.dal33t.powerfolder.branding;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Offer various helper methods for branding
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public abstract class AbstractBranding extends Loggable implements Branding {

    protected boolean loadTranslation(String brandingId) {
        // Load texts
        String translationFile = "Translation_en_" + brandingId + ".properties";
        if (Thread.currentThread().getContextClassLoader().getResourceAsStream(
            translationFile) != null)
        {
            Locale l = new Locale("en", brandingId);
            Translation.saveLocalSetting(l);
            Translation.resetResourceBundle();
            logInfo("Branding/Translation file loaded: " + translationFile);
            return true;
        }
        return false;
    }

    protected boolean loadClientPreConfig(Properties config) {
        InputStream in = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("Client.config");
        if (in != null) {
            ConfigurationLoader.loadPreConfiguration(in, config, true);
            logInfo("Branding/Preconfiguration file Client.config");
            return true;
        }
        return false;
    }
}
