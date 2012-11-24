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

import de.dal33t.powerfolder.util.StringUtils;

/**
 * Message to force the client to reload the config from a given URL.
 * <p>
 * TRAC #1799
 * 
 * @author sprajc
 */
public class ConfigurationLoadRequest extends Message {
    private static final long serialVersionUID = 2L;

    private String configURL;
    private String key;
    private String value;
    private Boolean replaceExisting;
    private boolean restartRequired;

    public ConfigurationLoadRequest(String configURL, Boolean replaceExisting,
        boolean restartRequired)
    {
        super();
        this.configURL = configURL;
        this.replaceExisting = replaceExisting;
        this.restartRequired = restartRequired;
    }

    public ConfigurationLoadRequest(String key, String value,
        Boolean replaceExisting, boolean restartRequired)
    {
        super();
        this.key = key;
        this.value = value;
        this.replaceExisting = replaceExisting;
        this.restartRequired = restartRequired;
    }

    public boolean isKeyValue() {
        return StringUtils.isBlank(configURL) && StringUtils.isNotBlank(key);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getConfigURL() {
        return configURL;
    }

    public Boolean isReplaceExisting() {
        return replaceExisting;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    @Override
    public String toString() {
        if (isKeyValue()) {
            return "SetConfig " + key + "=" + value + " , replace existing? "
                + replaceExisting + ", restart? " + restartRequired;
        }
        return "ReloadConfig from " + configURL + ", replace existing? "
            + replaceExisting + ", restart? " + restartRequired;
    }

}
