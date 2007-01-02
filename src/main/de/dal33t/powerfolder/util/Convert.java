package de.dal33t.powerfolder.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;

/** converts various stuff */
public class Convert {

    // no instances
    private Convert() {

    }
    // The local offset to UTC time in MS
    private static final long TIMEZONE_OFFSET_TO_UTC_MS = ((Calendar
        .getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(
        Calendar.DST_OFFSET)));

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
        for (int i = 0; i < b.length; i++) {
            w <<= 8;
            if (b[i] < 0) {
                w += b[i] + 256;
            } else {
                w += b[i];
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
     * FIXME Sometime produces diffrent result like comparing with
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
