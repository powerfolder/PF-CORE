package edu.kit.scc.dei.ecplean;

import java.net.URI;

public class ECPLeanTest {

    public static void main(String[] args) throws Exception {
        String idp = "https://idptest.scc.kit.edu/idp/profile/SAML2/SOAP/ECP";
        String sp = "https://bwlsdf.scc.kit.edu/auth/powerfolder";
        String un = "shib-pf0001@kit.edu";
        String pw = ""; // fowerpolder55146

        // Zum Testen habe ich wie besprochen von den Kollegen in Freiburg einen
        // IdP-Testaccount bekommen:
        // User: "idmtest1" Pass:"ni2vDBbG"
        sp = "https://tis-sp-01.tis.scc.kit.edu/powerfolder/";
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

        ECPAuthenticator auth = new ECPAuthenticator(un, pw, new URI(idp),
        // https://bwidm-idp.uni-konstanz.de/idp/profile/SAML2/SOAP/ECP
            new URI(sp));
        // https://tis-sp-01.tis.scc.kit.edu/powerfolder/
        String email = auth.authenticate();
        System.out.println("Email: " + email);
    }
}
