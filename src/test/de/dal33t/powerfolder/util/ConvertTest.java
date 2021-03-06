/*
 * Copyright 2004 - 2019 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.NodeManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConvertTest {

    @Test
    public void convert2BytesTest() {
        int number = 123456789;
        byte[] convertedNumber = Convert.convert2Bytes(number);
        byte[] expectedNumber = ByteBuffer.allocate(4).putInt(number).array();
        assertEquals(expectedNumber[0], convertedNumber[0]);
        assertEquals(expectedNumber[1], convertedNumber[1]);
        assertEquals(expectedNumber[2], convertedNumber[2]);
        assertEquals(expectedNumber[3], convertedNumber[3]);
    }

    @Test
    public void convert2IntTest() {
        byte[] arrayToConvert = {25, 32, 12, 22};
        assertEquals(421530646, Convert.convert2Int(arrayToConvert));

        byte[] anotherArray = {-12, 87, 11, 23};
        assertEquals(-195622121, Convert.convert2Int(anotherArray));
    }

    @Test(expected = NullPointerException.class)
    public void asMemberInfosArrayNullTest() {
        Member[] members = null;
        Convert.asMemberInfos(members);
    }

    @Test
    public void asMemberInfosArrayTest(){
        Controller controller = new Controller();

        MemberInfo firstMemberInfo = new MemberInfo("First nick", "First id", "First network");
        Member firstMember = new Member(controller, firstMemberInfo);

        MemberInfo secondMemberInfo = new MemberInfo("Second nick", "Second id", "Second network");
        Member secondMember = new Member(controller, secondMemberInfo);

        MemberInfo thirdMemberInfo = new MemberInfo("Third nick", "Third id", "Third network");
        Member thirdMember = new Member(controller, thirdMemberInfo);

        Member[] members = {firstMember, secondMember, thirdMember};

        MemberInfo[] memberInfos = Convert.asMemberInfos(members);

        assertEquals(memberInfos[0], firstMemberInfo);
        assertEquals(memberInfos[1], secondMemberInfo);
        assertEquals(memberInfos[2], thirdMemberInfo);

    }

    @Test(expected = NullPointerException.class)
    public void asMemberInfosListNullTest(){
        List members = null;
        Convert.asMemberInfos(members);
    }

    @Test
    public void asMemberInfosListTest() {
        Controller controller = new Controller();

        MemberInfo firstMemberInfo = new MemberInfo("First nick", "First id", "First network");
        Member firstMember = new Member(controller, firstMemberInfo);

        MemberInfo secondMemberInfo = new MemberInfo("Second nick", "Second id", "Second network");
        Member secondMember = new Member(controller, secondMemberInfo);

        MemberInfo thirdMemberInfo = new MemberInfo("Third nick", "Third id", "Third network");
        Member thirdMember = new Member(controller, thirdMemberInfo);

        List members = Arrays.asList(firstMember, secondMember, thirdMember);

        List memberInfos = Convert.asMemberInfos(members);

        assertEquals(memberInfos.get(0), firstMemberInfo);
        assertEquals(memberInfos.get(1), secondMemberInfo);
        assertEquals(memberInfos.get(2), thirdMemberInfo);
    }

    @Test
    public void convertToUTC() {
        Date date = new Date();
        long newDate = date.getTime() - (Calendar.getInstance().get(Calendar.ZONE_OFFSET)
                + Calendar.getInstance().get(Calendar.DST_OFFSET));
        assertEquals(newDate, Convert.convertToUTC(date));
    }

    @Test
    public void convertToGlobalPrecision() {
        Date date = new Date();
        long time = 1999999999999999999L;
        date.setTime(time);

        assertEquals(1999999999999998000L, Convert.convertToGlobalPrecision(date.getTime()));

    }
}