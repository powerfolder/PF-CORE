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
 * $Id: IdGenerator.java 20287 2012-11-24 23:04:06Z sprajc $
 */
package de.dal33t.powerfolder.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Simple mechanism to generate a unique id in space and time
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdGenerator {

    /**
     * Generates a randomly unique, base58 encoded id for a UUID. The UUID is
     * 128 bits (16 bytes) strong. String does NOT contain any special
     * characters NOR url incompatible chars.
     *
     * @see UUID
     * @return the base58 encoded uuid
     */
    public static String makeId() {
        String id = Base58.encode(makeIdBytes());
        // Remove the last == at the end
        return PathUtils.removeInvalidFilenameChars(
            id.substring(0, id.length() - 2)).replace("+", "");
    }


    /**
     * Generates a randomly unique, base58 encoded id for a UUID. The UUID is
     * 128 bits (16 bytes) strong. String does NOT contain any special
     * characters NOR url incompatible chars.
     *
     * @see UUID
     * @return the base58 encoded uuid
     */
    public static String makeFolderId() {
        // Version 2 of folder IDs
        return "2" + makeId();
    }
    
    /**
     * Generates a randomly unique, base58 encoded id for a UUID. The UUID is
     * 128 bits (16 bytes) strong. String does NOT contain any special
     * characters NOR url incompatible chars.
     *
     * @see UUID
     * @return the base58 encoded uuid
     */
    public static String makeFileId() {
        // Version 2 of File IDs
        return "1" + makeId();
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

    public static final String FILE_LINK_PREFIX = "fi";

    public static String generateFileLinkID(FileInfo fInfo) {
        String tmp = fInfo.getFolderInfo().id + "/" + fInfo.getRelativeName();
        byte[] buf = md5(tmp.getBytes(Convert.UTF8));
        return FILE_LINK_PREFIX + Base58.encode(buf);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element
     * <code>byte[]</code>.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest
     */
    private static byte[] md5(byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Returns a MessageDigest for the given <code>algorithm</code>.
     *
     * @param algorithm
     *            The MessageDigest algorithm name.
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns an MD5 MessageDigest.
     *
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getMd5Digest() {
        return getDigest("MD5");
    }
}
