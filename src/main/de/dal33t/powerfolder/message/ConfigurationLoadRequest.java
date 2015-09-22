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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.protocol.ConfigurationLoadRequestProto;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Message to force the client to reload the config from a given URL.
 * <p>
 * TRAC #1799
 *
 * @author sprajc
 */
public class ConfigurationLoadRequest extends Message
  implements D2DMessage
{
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

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2DMessage(AbstractMessage mesg)
    {
      if(mesg instanceof ConfigurationLoadRequestProto.ConfigurationLoadRequest)
        {
          ConfigurationLoadRequestProto.ConfigurationLoadRequest proto =
            (ConfigurationLoadRequestProto.ConfigurationLoadRequest)mesg;

          this.configURL       = proto.getConfigURL();
          this.key             = proto.getKey();
          this.value           = proto.getValue();
          this.replaceExisting = proto.getReplaceExisting();
          this.restartRequired = proto.getRestartRequired();
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2DMessage()
    {
      ConfigurationLoadRequestProto.ConfigurationLoadRequest.Builder builder =
        ConfigurationLoadRequestProto.ConfigurationLoadRequest.newBuilder();

      builder.setClassName("ConfigurationLoadRequest");
      builder.setConfigURL(this.configURL);
      builder.setKey(this.key);
      builder.setValue(this.value);
      builder.setReplaceExisting(this.replaceExisting);
      builder.setRestartRequired(this.restartRequired);

      return builder.build();
    }

}
