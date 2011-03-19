package de.dal33t.powerfolder.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

public class Bitly {
    private static final Logger LOG = Logger.getLogger(Bitly.class.getName());

    private static final int REQUEST_TIMEOUT = 1000 * 10;
    public static final String PARAM_LOGIN = "login";
    public static final String PARAM_APIKEY = "apiKey";
    public static final String PARAM_LONGURL = "longUrl";
    public static final String PARAM_FORMAT = "format";

    public static String shorten(String longURL) {
        URLConnection con = null;
        URL apiURL = null;
        try {
            StringBuilder b = new StringBuilder("http://api.bit.ly/v3/shorten?");
            b.append(PARAM_LOGIN).append("=powerfolder");
            b.append('&').append(PARAM_APIKEY).append(
                "=R_eb84fdda6be72567dff32af7bc68f688");
            b.append('&').append(PARAM_LONGURL).append('=');
            b.append(longURL);
            b.append('&').append(PARAM_FORMAT).append("=txt");

            apiURL = new URL(b.toString());
            con = apiURL.openConnection();

            con.setConnectTimeout(REQUEST_TIMEOUT);
            con.setReadTimeout(REQUEST_TIMEOUT);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.connect();

            // Read.
            String encoding = con.getContentEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }
            int resCode = ((HttpURLConnection) con).getResponseCode();
            String resMsg = ((HttpURLConnection) con).getResponseMessage();
            if (resCode != 200) {
                LOG.warning("Unable to shorten URL " + longURL + " with "
                    + apiURL + ". Response code: " + resCode + ": " + resMsg);
                return null;
            }
            Reader r = new InputStreamReader(new BufferedInputStream(con
                .getInputStream()), encoding);
            StringBuilder res = new StringBuilder();
            int c;
            while ((c = r.read()) != -1) {
                res.append((char) c);
            }
            String shortURL = res.toString();
            return shortURL.trim();
        } catch (IOException e) {
            try {
                if (con != null) {
                    con.getInputStream().close();
                }
            } catch (IOException e1) {
                // Ignore
            }
            LOG.severe("Unable to shorten URL " + longURL + " with " + apiURL
                + ". " + e.toString());
            return null;
        }
    }
}
