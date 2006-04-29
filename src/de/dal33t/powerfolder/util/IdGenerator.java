/* $Id: IdGenerator.java,v 1.2 2004/09/24 03:37:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * Simple mechanismn to generate a unique id in space and time
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdGenerator {
    private static String addressID;
    private static String timeID;
    private static short loopCounter = 0;

    private static boolean isInitialized = false;

    /**
     * Encode the information in base64.
     *
     * Base64 Alphabet (0-9, 10=A, 35=Z, 36=a, 61=z, 62='+', 63='#'). 
     *
     * @param bytes byte Byte array to be base64-encoded
     * @return java.lang.String Base64-encoded String of the input
     */
    private static String encodeBytesInBase64(byte[] bytes) {

        // max. 8 Ziffern
        char[] chars = { '_', '_', '_', '_', '_', '_', '_', '_' };

        // 8 x
        for (int i = 7; i >= 0; i--) {

            int z = bytes[5] & 0x3F;
            // z in Ziffer umwandeln
            if (z < 10) {
                chars[i] = (char) ('0' + z);
            } else if (z < 36) {
                chars[i] = (char) ('A' + z - 10);
            } else if (z < 62) {
                chars[i] = (char) ('a' + z - 36);
            } else if (z == 62) {
                chars[i] = '+';
            } else {
                chars[i] = '#';
            }

            // Shiften...
            for (int j = 5; j > 0; j--) {
                bytes[j] =
                    (byte) ((bytes[j] >> 6) + ((bytes[j - 1] & 0x3F) * 4));
            }
        }
        return new String(chars);
    }

    /**
     * Get the counter value as a signed short. 
     * @return short
     */
    private static synchronized short getLoopCounter() {
        return loopCounter++;
    }

    /**
     * Initializes the per-JVM static part of the Object ID.
     */
    private static void initialize() {

        try {
            // Prepare a buffer to hold the 6 bytes for the addressID 
            byte[] bytes6 = new byte[6];

            // Get the local IP address as a byte array 
            byte[] bytesIP = InetAddress.getLocalHost().getAddress();

            // Copy the 2 less significant bytes of the IP address 
            bytes6[5] = bytesIP[2];
            bytes6[4] = bytesIP[3];

            // Get the memory address for this object 
            int memAdr = System.identityHashCode(bytesIP);

            // Prepare a buffer to hold the 4 less significant bytes of the addr 
            byte[] bytes4 = new byte[4];

            // Convert the memory address into a byte array 
            toFixSizeByteArray(new BigInteger(String.valueOf(memAdr)), bytes4);
            bytes6[3] = bytes4[0];
            bytes6[2] = bytes4[1];
            bytes6[1] = bytes4[2];
            bytes6[0] = bytes4[3];

            // Encode the information in base64 
            addressID = encodeBytesInBase64(bytes6);

            isInitialized = true;
        } catch (Exception ignore) {
        }
    }

    /**
     * Generates a new Object ID.
     * 
     * @return A new (globally unique) Object ID.
     */
    public static String makeId() {
        // Initialsierung
        if (!isInitialized) {
            initialize();
        }

        // Prepare a buffer to hold the 6 bytes for the timeID 
        byte[] bytes6 = new byte[6];

        // Get the current time 
        long timeNow = System.currentTimeMillis();

        // Ignore the most 4 significant bytes 
        timeNow = (int) timeNow & 0xFFFFFFFF;

        // Prepare a buffer to hold the 4 less significant bytes of the time 
        byte[] bytes4 = new byte[4];

        // Convert the memory address into a byte array 
        toFixSizeByteArray(new BigInteger(String.valueOf(timeNow)), bytes4);
        bytes6[5] = bytes4[0];
        bytes6[4] = bytes4[1];
        bytes6[3] = bytes4[2];
        bytes6[2] = bytes4[3];

        // Get the current counter reading 
        short counter = getLoopCounter();

        // Prepare a buffer to hold the 2 bytes of the counter 
        byte[] bytes2 = new byte[2];

        // Convert the counter into a byte array 
        toFixSizeByteArray(new BigInteger(String.valueOf(counter)), bytes2);
        bytes6[1] = bytes2[0];
        bytes6[0] = bytes2[1];

        // Encode the information in base64 
        timeID = encodeBytesInBase64(bytes6);

        // Return the unique ID 
        return timeID + addressID;

    }

    /**
     * This method transforms Java BigInteger type into a fix size byte array 
     * containing the two's-complement representation of the integer. 
     * The byte array will be in big-endian byte-order: the most significant 
     * byte is in the zeroth element. 
     * If the destination array is shorter then the BigInteger.toByteArray(), 
     * the the less significant bytes will be copy only. 
     * If the destination array is longer then the BigInteger.toByteArray(), 
     * destination will be left padded with zeros. 
     * 
     * @param bigInt java.math.BigInteger
     * @param destination byte[]
     */
    private static void toFixSizeByteArray(
        java.math.BigInteger bigInt,
        byte[] destination) {

        // Prepare the destination 
        for (int i = 0; i < destination.length; i++) {
            destination[i] = 0x00;
        }

        // Convert the BigInt to a byte array 
        byte[] source = bigInt.toByteArray();

        // Copy only the fix size length 
        if (source.length <= destination.length) {
            for (int i = 0; i < source.length; i++) {
                destination[destination.length - source.length + i] = source[i];
            }
        } else {
            for (int i = 0; i < destination.length; i++) {
                destination[i] = source[source.length - destination.length + i];
            }
        }
    }

}
