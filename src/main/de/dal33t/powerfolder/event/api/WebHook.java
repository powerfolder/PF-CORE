/*
 * Copyright 2004 - 2017 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.event.api;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * PFS-1766: Basic class to handle events to web hooks at external URLs.
 *
 * @author Christian Sprajc
 */
public abstract class WebHook extends PFComponent {
    private final String url;
    private HttpClient client;
    private List<NameValuePair> parameters;

    public WebHook(Controller controller, ConfigurationEntry urlEntry) {
        this(controller, urlEntry.getValue(controller));
    }

    public WebHook(Controller controller, String url) {
        super(controller);
        this.url = url;
        if (StringUtils.isNotBlank(url)) {
            this.parameters = new ArrayList<>();
            this.client = Util.createHttpClientBuilder(controller).build();
        }
    }

    protected void addParameter(String name, String value) {
        if (parameters != null) {
            parameters.add(new BasicNameValuePair(name, value));
        }
    }

    protected void addParameter(String name, Object value) {
        if (parameters != null) {
            parameters.add(new BasicNameValuePair(name, value.toString()));
        }
    }

    public final void happened(boolean executeInBackgroud) {
        if (client == null) {
            return;
        }
        if (executeInBackgroud) {
            getController().getIOProvider().startIO(() -> execute());
        } else {
            execute();
        }
    }

    private void execute() {
        try {
            HttpPost request = new HttpPost(url);
            if (parameters != null) {
                parameters.add(new BasicNameValuePair("event", getClass().getSimpleName()));
                request.setEntity(new UrlEncodedFormEntity(parameters, Convert.UTF8));
            }
            logInfo("Executing " + getClass().getSimpleName() + " to " + url);
            client.execute(request);
        } catch (IOException e) {
            logWarning("Exception while executing POST to " + url + " : " + e);
        } catch (RuntimeException e) {
            logWarning("RuntimeException while executing POST to " + url + " : " + e, e);
        }
    }
}
