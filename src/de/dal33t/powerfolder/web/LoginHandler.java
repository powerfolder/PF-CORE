package de.dal33t.powerfolder.web;

import java.io.StringWriter;
import java.net.InetAddress;
import java.util.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.IdGenerator;

public class LoginHandler extends PFComponent implements Handler {
    private String password;
    private String username;
    private final String COOKIE_USERNAME = "Username";
    private final String COOKIE_SESSIONID = "SessionID";
    /** make it secure, one global session acces for one browser */
    private String sessionID;
    /** IP where the session is from, access from one IP address */
    private InetAddress IP;
    /** invalidate SessionID after 15 minutes */
    private GregorianCalendar lastAccess;

    public LoginHandler(Controller controller) {
        super(controller);
        initProperties();
    }

    private void initProperties() {
        Properties props = getController().getConfig();
        String usernameStr = props.getProperty(WebInterface.USERNAME_SETTING);
            
        if (usernameStr != null && usernameStr.trim().length() > 0) {
            username = usernameStr.trim();
        } else {
            username = getController().getMySelf().getNick();
        }
        String passwordStr = props.getProperty(WebInterface.PASSWORD_SETTING);
            
        if (passwordStr != null && passwordStr.trim().length() > 0) {
            password = passwordStr.trim();
        } else {
            password = getController().getMySelf().getNick();
        }
    }

    public HTTPResponse getPage(HTTPRequest httpRequest) {

        /* lets make a Context and put data into it */
        VelocityContext context = new VelocityContext();
        context.put("PowerFolderVersion", Controller.PROGRAM_VERSION);
        if (httpRequest.getQueryParams() != null) {
            if (httpRequest.getQueryParams().containsKey("Username")
                && httpRequest.getQueryParams().containsKey("Password"))
            {
                String usernameEntered = httpRequest.getQueryParams()
                    .get("Username");
                String passwordEntered = httpRequest.getQueryParams()
                    .get("Password");                
                
                if (usernameEntered.equals(username)
                    && passwordEntered.equals(password))
                { // login succes will redirect to the root ("/")
                    return loginSucces(httpRequest);
                }
                context.put("ShowError", true);
                context.put("ErrorMessage",
                    "You specified an incorrect Username or Password");
            }
        }
        /* lets render a template */
        StringWriter writer = new StringWriter();
        try {
            Velocity.mergeTemplate("login.vm", Velocity.ENCODING_DEFAULT,
                context, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new HTTPResponse(writer.toString().getBytes());
    }

    private HTTPResponse loginSucces(HTTPRequest httpRequest) {     
        saveNewSessionInfo(httpRequest);
        Map<String, String> cookies = new HashMap<String, String>();
        HTTPResponse response = new HTTPResponse();
        response.redirectToRoot();
        cookies.put(COOKIE_SESSIONID, sessionID);
        cookies.put(COOKIE_USERNAME, username);
        response.setCookies(cookies);
        return response;
    }

    private void saveNewSessionInfo(HTTPRequest request) {
        IP = request.getSocket().getInetAddress();
        sessionID = generateSessionID();
        lastAccess = new GregorianCalendar();
    }

    private String generateSessionID() {
        return IdGenerator.makeId();
    }

    /** false if session expired or invalid sessionID, else true */
    boolean checkSession(Map<String, String> cookies, InetAddress inetAddres) {
        if (lastAccess == null) {
            return false;
        }
        
        Calendar calNow = new GregorianCalendar();
        calNow.add(Calendar.MINUTE, -15);

        if (calNow.before(lastAccess)) {
            InetAddress address = inetAddres;
            if (address.equals(IP)) {

                if (cookies.containsKey(COOKIE_USERNAME)
                    && cookies.containsKey(COOKIE_SESSIONID))
                {
                    String current_username = cookies.get(COOKIE_USERNAME);
                    String current_sessionID = cookies.get(COOKIE_SESSIONID);
                    if (current_username.equals(username)
                        && current_sessionID.equals(sessionID))
                    {
                        // authorized
                        lastAccess = new GregorianCalendar();
                        return true;
                    }
                }
            }
        }
        IP = null;
        lastAccess = null;
        return false;
    }
}
