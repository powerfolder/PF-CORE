package de.dal33t.powerfolder.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import de.dal33t.powerfolder.Constants;

/**
 * Helper class to subscribe to the powerfolder newsletter.
 * <p>
 * TODO Complete this.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Newsletter {
    private static final Logger LOG = Logger.getLogger(Newsletter.class);

    private Newsletter() {
    }

    /**
     * Subscribes a email to the newsletter.
     * 
     * @param email
     *            the email that should be subscribed
     * @return true if subscription has succeeded, otherwise false.
     */
    public static final boolean subscribe(String email) {
        Reject.ifBlank(email, "Email is blank");
        Reject.ifFalse(Util.isValidEmail(email), "Email is invalid: " + email);

        try {
            String s = Constants.POWERFOLDER_URL;
            s += "/?";
            s += URLEncoder.encode("edit[sn_email_10]", "UTF-8");
            s += "=";
            s += URLEncoder.encode(email, "UTF-8");
            s += "&";
            s += URLEncoder.encode("edit[sn_subscribe_10]", "UTF-8");
            s += "=";
            s += URLEncoder.encode("Subscribe", "UTF-8");
            s += "&";
            s += URLEncoder.encode("edit[form_id]", "UTF-8");
            s += "=";
            s += URLEncoder.encode("theme_sn_form", "UTF-8");
            s += "&";
            s += URLEncoder.encode("sn_10", "UTF-8");
            s += "=";
            s += URLEncoder.encode("Submit", "UTF-8");

            URL u = new URL(Constants.POWERFOLDER_URL + "/");
            URLConnection c = u.openConnection();

            c.setRequestProperty("edit[sn_email_10]", email);
            c.setRequestProperty("edit[sn_subscribe_10]", "Subscribe");
            c.setRequestProperty("edit[form_id]", "theme_sn_form");
            c.setRequestProperty("sn_10", "Submit");

            InputStream in = c.getInputStream();
            // http://www.powerfolder.com/?edit%5Bsn_email_10%5D=totmacher%40powerfolder.com&edit%5Bsn_subscribe_10%5D=Subscribe&sn_10=Submit&edit%5Bform_id%5D=theme_sn_form
            // http://www.powerfolder.com/?edit[sn_email_10]=totmacher%40powerfolder.com&edit[sn_subscribe_10]=Subscribe&edit[form_id]=theme_sn_form
            System.err.println(u);
            // Perform HTTP request GET
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            StreamUtils.copyToStream(in, bOut);
            in.close();
            System.out.println(new String(bOut.toByteArray()));
            return true;
        } catch (MalformedURLException e) {
            LOG.warn("Unable to subscribe to newsletter", e);
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Unable to subscribe to newsletter", e);
        } catch (IOException e) {
            LOG.warn("Unable to subscribe to newsletter", e);
        }
        return false;
    }
}
