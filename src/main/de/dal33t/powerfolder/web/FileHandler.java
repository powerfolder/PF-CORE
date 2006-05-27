package de.dal33t.powerfolder.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import de.dal33t.powerfolder.PFComponent;

/**
 * Gives access to a file from the jar to the WebInterface. eg:
 * <code>/icon/powerfolder.jpg</code> if this file exists in the jar it will
 * be "served".
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FileHandler extends PFComponent {
    public HTTPResponse doGet(HTTPRequest request) {
        String filename = request.getFile();

        if (filename.indexOf("..") != -1) {
            // no path below the root allowed (or any path with ..)
            return null;
        }
        // strip leading "/"
        if (filename.startsWith("/")) {
            filename = filename.substring(1);
        }
        try {
            URL url = ClassLoader.getSystemResource(filename);
            if (url == null) {
                log().debug("file not found: " + filename);
                return null;
            }
            URLConnection connection = url.openConnection();
            // this returns not a valid date:
            // long moddate = connection.getDate();
            long contenstLength = connection.getContentLength();
            // log().debug(
            // "file found: " + filename + " " + moddate + " "
            // + contenstLength);

            if (contenstLength > -1) {
                InputStream in = connection.getInputStream();
                HTTPResponse response = new HTTPResponse(in);
                response.setSize(contenstLength);
                response.setContentType(HTTPResponse.getMimeType(filename));
                return response;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        log().debug("file not found: " + filename);
        return null;
    }
}
