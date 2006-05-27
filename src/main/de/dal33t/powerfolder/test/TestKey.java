package de.dal33t.powerfolder.test;

import java.io.*;
import java.security.*;

class TestKey {

    public static void main(String[] args) {

        /* Test generating and verifying a DSA signature */

        try {

            /* generate a key pair */

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024, SecureRandom.getInstance("SHA1PRNG"));
            KeyPair keyPair = keyGen.generateKeyPair();

            /*
             * create a Signature object to use for signing and verifying
             */

            Signature dsa = Signature.getInstance("SHA/DSA");

            /* initialize the Signature object for signing */

            PrivateKey privacteKey = keyPair.getPrivate();

            dsa.initSign(privacteKey);

            /* Update and sign the data */

            FileInputStream fis = new FileInputStream(args[0]);
            byte b;
            while (fis.available() != 0) {
                b = (byte) fis.read();
                dsa.update(b);
            };

            fis.close();

            /*
             * Now that all the data to be signed has been read in, sign it
             */
            byte[] sig = dsa.sign();

            /* Verify the signature */

            /* Initialize the Signature object for verification */
            PublicKey publicKey = keyPair.getPublic();
            dsa.initVerify(publicKey);

            /* Update and verify the data */
            fis = new FileInputStream(args[0]);
            while (fis.available() != 0) {
                b = (byte) fis.read();
                dsa.update(b);
            }

            fis.close();

            boolean verifies = dsa.verify(sig);

            System.out.println("signature verifies: " + verifies);

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }

    }

}