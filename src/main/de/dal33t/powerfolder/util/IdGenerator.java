/* $Id: IdGenerator.java,v 1.2 2004/09/24 03:37:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.util.UUID;

/**
 * Simple mechanism to generate a unique id in space and time
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdGenerator {

    /**
     * Generates a randomly unique, base64 encoded id for a UUID. The UUID is
     * 128 bits (16 bytes) strong. The returned string is 22 characters long.
     * 
     * @see UUID
     * @return the base64 encoded uuid
     */
    public static String makeId() {
        String id = Base64.encodeBytes(makeIdBytes());
        // Remove the last == at the end
        return id.substring(0, id.length() - 2);
    }

    /**
     * @return a random UUID as byte array (16 bytes strong)
     */
    public static byte[] makeIdBytes() {
        UUID uid = UUID.randomUUID();
        byte[] arr = new byte[16];

        long msb = uid.getMostSignificantBits();
        arr[0] = (byte) ((msb >> 56) & 0xFF);
        arr[1] = (byte) ((msb >> 48) & 0xFF);
        arr[2] = (byte) ((msb >> 40) & 0xFF);
        arr[3] = (byte) ((msb >> 32) & 0xFF);
        arr[4] = (byte) ((msb >> 24) & 0xFF);
        arr[5] = (byte) ((msb >> 16) & 0xFF);
        arr[6] = (byte) ((msb >> 8) & 0xFF);
        arr[7] = (byte) (msb & 0xFF);

        long lsb = uid.getLeastSignificantBits();
        arr[8] = (byte) ((lsb >> 56) & 0xFF);
        arr[9] = (byte) ((lsb >> 48) & 0xFF);
        arr[10] = (byte) ((lsb >> 40) & 0xFF);
        arr[11] = (byte) ((lsb >> 32) & 0xFF);
        arr[12] = (byte) ((lsb >> 24) & 0xFF);
        arr[13] = (byte) ((lsb >> 16) & 0xFF);
        arr[14] = (byte) ((lsb >> 8) & 0xFF);
        arr[15] = (byte) (lsb & 0xFF);

        return arr;
    }
}
