package de.dal33t.powerfolder.net;

import java.util.Hashtable;
import de.dal33t.powerfolder.PFComponent;

/*
 * The ErrorManager manages the errors 
 * recieved from the updated dyndns server
 * 
 * @author albena roshelova
 */
public class ErrorManager extends PFComponent {

    // display
    public static final int NO_ERROR = 0;
    public static final int WARN = 1;
    public static final int ERROR = 2;
    public static final int UNKNOWN = 3;

    public Hashtable errors;

    public ErrorManager() {
        errors = new Hashtable();
    }

    public int getType(String errorCode) {

        ErrorInfo info = (ErrorInfo) errors.get(errorCode);
        if (info != null) {
            return info.getType();
        }
        log().verbose("Uknown errorCode received: " + errorCode);
        return UNKNOWN;

    }

    public int getCode(String errorCode) {
        ErrorInfo info = (ErrorInfo) errors.get(errorCode);
        if (info != null) {
            return info.getCode();
        }
        log().verbose("Uknown errorCode received: " + errorCode);
        return UNKNOWN;
    }

    public String getText(String errorCode) {
        ErrorInfo info = (ErrorInfo) errors.get(errorCode);
        if (info != null) {
            return info.getText();
        }
        log().verbose("Uknown errorCode received: " + errorCode);
        return "";
    }

    public String getShortText(String errorCode) {
        ErrorInfo info = (ErrorInfo) errors.get(errorCode);
        if (info != null) {
            return info.getShortText();
        }
        return "";
    }
}