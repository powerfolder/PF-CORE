package edu.kit.scc.dei.ecplean;

import java.net.URI;

import de.dal33t.powerfolder.util.StringUtils;

public class ECPLeanTest {

    public static void main(String[] args) {
        String idp = "";
        String sp = "";
        String un = "";
        String pw = "";

        if (args.length < 4) {
            System.err.println("Please specify command line options");
            return;
        }
        if (args.length >= 1) {
            sp = args[0];
        }
        if (args.length >= 2) {
            idp = args[1];
        }
        if (args.length >= 3) {
            un = args[2];
        }
        if (args.length >= 4) {
            pw = args[3];
        }

        System.out.println();
        System.out.println();
        System.out.println("USING:");
        System.out.println("Service Provider URL : " + sp);
        System.out.println("Identity Provider URL: " + idp);
        System.out.println("Username: " + un);
        System.out.println("Password: " + pw);
        System.out.println();
        System.out.println();

        String username;
        String token;
        String error;
        try {
            ECPAuthenticator auth = new ECPAuthenticator(un, pw, new URI(idp),
                new URI(sp));
            String[] result = auth.authenticate();
            username = result[0];
            token = result[1];
            error = null;
        } catch (ECPUnauthorizedException e) {
            username = null;
            token = null;
            error = "Username or password wrong -- " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            username = null;
            token = null;
            error = e.toString();
        }

        System.out.println();
        System.out.println("Username: " + username);
        System.out.println("Token: " + token);
        System.out.println();

        if (StringUtils.isNotBlank(username)) {
            System.out.println("--------------------");
            System.out.println();
            System.out.println("RESULT: OK");
            System.out.println();
            System.out.println("--------------------");
        } else {
            System.err.println("--------------------");
            System.err.println();
            System.err.println("RESULT: FAIL (" + error + ")");
            System.err.println();
            System.err.println("--------------------");
        }
    }
}
