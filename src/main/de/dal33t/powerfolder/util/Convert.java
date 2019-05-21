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
package de.dal33t.powerfolder.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeManager;

/** converts various stuff */
public class Convert {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    private static final Logger log = Logger.getLogger(Convert.class.getName());

    // no instances
    private Convert() {
    }

    // The local offset to UTC time in MS
    private static final long TIMEZONE_OFFSET_TO_UTC_MS = Calendar
        .getInstance().get(Calendar.ZONE_OFFSET)
        + Calendar.getInstance().get(Calendar.DST_OFFSET);

    /**
     * Converts an int to a 4 bytes arrays
     *
     * @param i
     * @return
     */
    public static byte[] convert2Bytes(int i) {
        byte[] b = new byte[4];

        b[3] = (byte) (i & 0xFF);
        b[2] = (byte) (0xFF & (i >> 8));
        b[1] = (byte) (0xFF & (i >> 16));
        b[0] = (byte) (0xFF & (i >> 24));
        return b;
    }

    /**
     * Converts an arry of bytes to an int
     *
     * @param b
     * @return
     */
    public static int convert2Int(byte[] b) {
        int w = 0;
        for (byte aB : b) {
            w <<= 8;
            if (aB < 0) {
                w += aB + 256;
            } else {
                w += aB;
            }
        }
        return w;
    }

    /**
     * Converts a array of members into a array of memberinfos calling the
     * getInfo method on each
     *
     * @param members
     * @return
     */
    public static MemberInfo[] asMemberInfos(Member[] members) {
        if (members == null) {
            throw new NullPointerException("Memebers is null");
        }
        MemberInfo[] memberInfos = new MemberInfo[members.length];
        for (int i = 0; i < members.length; i++) {
            memberInfos[i] = members[i].getInfo();
        }
        return memberInfos;
    }

    /**
     * Converts a list of members into a list of memberinfos calling the getInfo
     * method on each
     *
     * @param members
     * @return
     */
    public static List<MemberInfo> asMemberInfos(Collection<Member> members) {
        if (members == null) {
            throw new NullPointerException("Memebers is null");
        }
        List<MemberInfo> memberInfos = new ArrayList<MemberInfo>(members.size());
        for (Member member : members) {
            memberInfos.add(member.getInfo());
        }
        return memberInfos;
    }

    /**
     * Converts a date to the value in UTC
     *
     * @param date
     * @return
     */
    public static long convertToUTC(Date date) {
        return date.getTime() - TIMEZONE_OFFSET_TO_UTC_MS;
    }

    /**
     * Chops a date (in MS) to a (lower) precision to make cross plattform
     * modified values of files comparable. All millisecond precision will be
     * lost
     * <p>
     * FIXME Sometime produces different result like comparing with
     * <code>Util#equalsFileDateCrossPlattform(long, long)</code>.
     *
     * @see Util#equalsFileDateCrossPlattform(long, long)
     * @param date
     *            the date to convert
     * @return the date in less precision
     */
    public static long convertToGlobalPrecision(long date) {
        return date / 2000 * 2000;
    }
}
