package de.dal33t.powerfolder.web;

public interface HTTPConstants {    
        public static final String HTTP_GET = "GET";
        public static final String HTTP_HEAD = "HEAD";
        public static final int HTTP_OK = 200;
        public static final int HTTP_NOT_FOUND = 404;
        public static final int HTTP_BAD_METHOD = 405;    
        public final static String COOKIE_EXPIRATION_DATE_FORMAT = "EEE',' dd-MMM-yyyy HH:mm:ss 'GMT'";
        /** End Of Line*/
        public final static byte[] EOL = {(byte) '\r', (byte) '\n'};
}
