/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
*
* $Id$
*/
package de.dal33t.powerfolder.test;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

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
            }

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