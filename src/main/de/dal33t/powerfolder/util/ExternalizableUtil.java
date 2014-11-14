/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: ByteSerializer.java 13415 2010-08-16 14:13:05Z tot $
 */
package de.dal33t.powerfolder.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * Helpers an utility methods for handling {@link Externalizable}
 *
 * @author sprajc
 */
public class ExternalizableUtil {

    private ExternalizableUtil() {
    }

    /**
     * Writes the string into the {@link ObjectOutput} and handles possible null
     * values. Note: This method produces a slight more overhead than
     * {@link ObjectOutput#writeUTF(String)}. Use this only if the
     * {@link String} can become null
     *
     * @param out
     * @param value
     * @throws IOException
     */
    public static void writeString(ObjectOutput out, String value)
        throws IOException
    {
        out.writeBoolean(value != null);
        if (value != null) {
            out.writeUTF(value);
        }
    }

    public static String readString(ObjectInput in) throws IOException {
        boolean notNull = in.readBoolean();
        if (!notNull) {
            return null;
        }
        return in.readUTF();
    }

    public static void writeDate(ObjectOutput out, Date date)
        throws IOException
    {
        if (date != null) {
            out.writeLong(date.getTime());
        } else {
            out.writeLong(-1);
        }
    }

    public static Date readDate(ObjectInput in) throws IOException {
        long time = in.readLong();
        if (time == -1) {
            return null;
        }
        return new Date(time);
    }

    public static void writeAddress(ObjectOutput out, InetSocketAddress value)
        throws IOException
    {
        String str = null;
        if (value != null) {
            InetAddress addr = value.getAddress();
            if (addr != null) {

                str = NetworkUtil.getHostAddressNoResolve(addr);
                str += ":";
                if (!value.isUnresolved()) {
                    str += Base64.encodeBytes(addr.getAddress());
                }
                str += ":";
                str += value.getPort();
            }
        }
        out.writeBoolean(str != null);
        if (str != null) {
            out.writeUTF(str);
        }
    }

    public static InetSocketAddress readAddress(ObjectInput in)
        throws IOException
    {
        boolean filled = in.readBoolean();
        if (!filled) {
            return null;
        }
        String str = in.readUTF();
        String[] addAndPort = str.split(":");
        if (addAndPort.length < 2) {
            return null;
        }
        if (addAndPort.length == 2) {
            return new InetSocketAddress(addAndPort[0], Integer.valueOf(
                addAndPort[1]).intValue());
        }
        String hostname = addAndPort[0];
        int port = Integer.valueOf(addAndPort[2]);
        if (StringUtils.isNotBlank(addAndPort[1])) {
            byte[] ip = Base64.decode(addAndPort[1]);
            InetAddress addr = InetAddress.getByAddress(hostname, ip);
            return new InetSocketAddress(addr, port);
        } else {
            return new InetSocketAddress(hostname, port);
        }
    }

    /**
     * Writes a {@link FolderInfo} handling possible null values.
     *
     * @param out
     * @param foInfo
     * @throws IOException
     */
    public static void writeFolderInfo(ObjectOutput out, FolderInfo foInfo)
        throws IOException
    {
        out.writeBoolean(foInfo != null);
        if (foInfo != null) {
            foInfo.writeExternal(out);
        }
    }

    /**
     * @param in
     * @return the {@link FolderInfo} or null
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static FolderInfo readFolderInfo(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        if (in.readBoolean()) {
            return FolderInfo.readExt(in);
        }
        return null;
    }
}
