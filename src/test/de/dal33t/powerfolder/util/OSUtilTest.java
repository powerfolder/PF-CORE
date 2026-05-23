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
package de.dal33t.powerfolder.util.os;

import junit.framework.TestCase;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URL;

/**
 * Simple tests for {@link OSUtil#configureTruststore()}.
 */
public class OSUtilTest extends TestCase {

    /**
     * Integration check: after configureTruststore() we should be able
     * to open an HTTPS connection to a well-known site with a public CA.
     */
    public void testHttpsConnectionAfterConfigureTruststore() throws Exception {
        boolean result = OSUtil.configureTruststore();
        assertTrue("configureTruststore() should succeed", result);

        URL url = new URL("https://www.google.com");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        assertEquals("Expected HTTP 200 OK", 200, code);

        InputStream in = conn.getInputStream();
        assertNotNull("Response stream should not be null", in);
        in.close();
        conn.disconnect();
    }


    public void testHttpsConnectionToPrepsysh() throws Exception {
        assertTrue("configureTruststore() should succeed", OSUtil.configureTruststore());

        URL url = new URL("https://prepsysh.nas.lrz.de/");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        // we don't know the exact status code in advance, but must be >=200 and <500
        assertTrue("Unexpected HTTP response code from prepsysh: " + code, code >= 200 && code < 500);

        InputStream in = conn.getInputStream();
        assertNotNull("Response stream from prepsysh should not be null", in);
        in.close();
        conn.disconnect();
    }
}
