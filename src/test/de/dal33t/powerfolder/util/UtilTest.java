/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class UtilTest extends TestCase {

    public void testEqualsRelativeNameNull() {
        assertFalse(Util.equalsRelativeName("Test", null));
        assertFalse(Util.equalsRelativeName(null, "This is a test"));
        assertTrue(Util.equalsRelativeName(null,null));
    }

    public void testEqualsRelativeDirect() {
        assertTrue(Util.equalsRelativeName("Test", "Test"));
        assertTrue(Util.equalsRelativeName("This is a test string", "This is a test string"));

        String someString = "Asdasd";
        String anotherString = someString;
        assertTrue(Util.equalsRelativeName(someString, anotherString));

        assertTrue(Util.equalsRelativeName("",""));
        assertTrue(Util.equalsRelativeName("   ", "   "));

        assertTrue(Util.equalsRelativeName("1234","1234"));
        assertTrue(Util.equalsRelativeName("!@#$%^&*(){}|","!@#$%^&*(){}|"));
    }

    public void testEqualsRelativeCase() {
        assertTrue(Util.equalsRelativeName("Test","test"));
        assertTrue(Util.equalsRelativeName("ThIsIsAtEsTsTrInG", "thisIsATestString"));
        assertTrue(Util.equalsRelativeName("   A", "   a"));
    }

    public void testEqualsRelativeFalse() {
        assertFalse(Util.equalsRelativeName("test", "anotherTest"));
        assertFalse(Util.equalsRelativeName("   a", "   "));
        assertFalse(Util.equalsRelativeName("", " "));
        assertFalse(Util.equalsRelativeName("2134","1234"));
        assertFalse(Util.equalsRelativeName("!@#$%^&*(){}|","!@#$%^&*(){}/"));
    }

    public void testEqualsTrue(){
        //Primitive types
        assertTrue(Util.equals(1,1));
        assertTrue(Util.equals(1234567890L, 1234567890L));
        assertTrue(Util.equals(1234.5678D, 1234.5678D));
        assertTrue(Util.equals((short) 123, (short) 123));
        assertTrue(Util.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(Util.equals(Double.NaN, Double.NaN));

        //Strings
        String someString = "Some string";
        assertTrue(Util.equals(someString, "Some string"));

        //Objects
        //Returns true since Member info overrides equals and compares by id
        MemberInfo firstMemberInfo = new MemberInfo("First nick", "First id", "First network");
        MemberInfo secondMemberInfo = new MemberInfo("First nick", "First id", "First network");

        assertTrue(Util.equals(firstMemberInfo, secondMemberInfo));

        Double firstNumber = new Double(1234.231);
        Double secondNumber = new Double(1234.231);
        assertTrue(Util.equals(firstNumber, secondNumber));

        Date date = new Date();
        Date anotherDate = date;
        assertTrue(Util.equals(date, anotherDate));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        assertTrue(Util.equals(calendar.getTime(), date));


        //Same object passed by reference
        int[] someArray = {1,2,3,4,5,6};
        int[] anotherArray = someArray;
        assertTrue(Util.equals(someArray, anotherArray));

        List firstList = Arrays.asList("TestOne", "TestTwo", "TestThree");
        List secondList = Arrays.asList("TestOne", "TestTwo", "TestThree");
        assertTrue(Util.equals(firstList, secondList));

        //Incremented after method executes
        int number = 5;
        assertTrue(Util.equals(number++, 5));
    }

    public void testEqualsFalse() throws ParseException {

        //Primitive types
        assertFalse(Util.equals(1,2));
        assertFalse(Util.equals(1234567890L, 9876543210L));
        assertFalse(Util.equals(2345.5D, 2345.6D));
        assertFalse(Util.equals((short) 5, (short) 6));
        assertFalse(Util.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(Util.equals(Double.NaN, Double.MAX_VALUE));

        //Different primitives
        assertFalse(Util.equals(1234L,1234D));
        assertFalse(Util.equals((short) 5, (int) 5));
        assertFalse(Util.equals(3, "3"));

        //Same values but a different object
        int[] firstArray = {1,2,3,4,5,6};
        int[] secondArray = {1,2,3,4,5,6};
        assertFalse(Util.equals(firstArray, secondArray));

        List firstList = Arrays.asList("1","2","3");
        List secondList = Arrays.asList("1","2","4");
        assertFalse(Util.equals(firstList, secondList));

        //Objects
        assertFalse(Util.equals(new Double(234), new Double(432)));
        assertFalse(Util.equals(new MemberInfo("1","4","3"), new MemberInfo("3", "2", "1")));
        assertFalse(Util.equals(new StringBuilder(), "File"));
        assertFalse(Util.equals(new Date(), new SimpleDateFormat("yyyy MM dd").parse("2019 08 31")));

        Date date = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 5);
        assertFalse(Util.equals(date, calendar.getTime()));


    }


    public void testEqualsNull(){
        assertFalse(Util.equals(null, "Test"));
        assertTrue(Util.equals(null, null));
        assertFalse(Util.equals("Test", null));

        String someString = null;

        assertFalse(Util.equals(new String(), someString));
    }


    public void testToString(){

        char[] chars = {'T','e','s','t'};
        assertEquals("Test", Util.toString(chars));

        char[] otherChars = {'1','2','3'};
        assertEquals("123", Util.toString(otherChars));

        char[] charsSpaces = {' ', ' ', ' ', 'a'};
        assertEquals("   a", Util.toString(charsSpaces));

        char[] charsEmpty = new char[0];
        assertEquals("", Util.toString(charsEmpty));

        char[] specialCharacters = {'\'', '\"', '$','\\', '#'};
        assertEquals("'\"$\\#", Util.toString(specialCharacters));
    }

    public void testToStringNull(){
        assertNull(Util.toString(null));
    }


    public void testToCharArray(){
        String someString = "";
        for (int index = 0; index < 1000; index++) {
            someString = someString + index;
        }
        char[] array = new char[1000];
        array = Util.toCharArray(someString);

        for (int index = 0; index < 1000; index++) {
            assertEquals(someString.charAt(index), array[index]);
        }

        String specialCharacters = "'][#$\"";
        char[] specialArray = Util.toCharArray(specialCharacters);
        assertEquals(specialArray[0], '\'');
        assertEquals(specialArray[1], ']');
        assertEquals(specialArray[2], '[');
        assertEquals(specialArray[3], '#');
        assertEquals(specialArray[4], '$');
        assertEquals(specialArray[5], '"');

        String spaces = "     ";
        char[] spacesArray = Util.toCharArray(spaces);
        for (int index = 0; index < spaces.length(); index++) {
            assertEquals(' ', spacesArray[index]);
        }

    }

    public void testToCharArrayNull(){
        assertNull(Util.toCharArray(null));
        String someString = null;
        assertNull(Util.toCharArray(someString));
    }

    public void testIsValidEmailEmpty(){

        assertFalse(Util.isValidEmail(""));
        String string = new String();
        assertFalse(Util.isValidEmail(string));
        assertFalse(Util.isValidEmail(null));

    }

    public void testIsValidEmail(){

        assertTrue(Util.isValidEmail("testing@test.com"));
        assertTrue(Util.isValidEmail("Test@de.test.com"));
        assertTrue(Util.isValidEmail("a@a.a"));
        assertTrue(Util.isValidEmail("asd123AsX@TesTiNg.CoM"));

        assertFalse(Util.isValidEmail("    "));
        assertFalse(Util.isValidEmail("Testing"));
        assertFalse(Util.isValidEmail("@test.com"));
        assertFalse(Util.isValidEmail(".de"));

        assertFalse(Util.isValidEmail("Test@Test"));
        assertFalse(Util.isValidEmail("tes\"t@test'.com"));

    }

    public void testGetLineFeedSeparator() {

        System.setProperty("line.separator","test");
        assertEquals("test", Util.getLineFeed());
        System.clearProperty("line.separator");

    }

    public void testUseSwarmingNullController() {

        Controller controller = null;

        Controller controllerNotNull = new Controller();
        MemberInfo memberInfo = new MemberInfo("One","Two","Three");
        Member member = new Member(controllerNotNull, memberInfo);

        try {
            Util.useSwarming(controller, member);
            fail("Did not throw null pointer exception when controller was null");
        } catch (NullPointerException e){
            //Ok, supposed to throw since controller is null
        }
        controllerNotNull.shutdown();
    }

    public void testUseSwarmingNullMember() {
        Controller controllerNotNull = new Controller();
        Member member = null;

        try {
            Util.useSwarming(controllerNotNull, member);
            fail("Did not throw null pointer exception when member was null");
        } catch (NullPointerException e){
            //Ok, supposed to throw since member is null
        }
    }

    public void testUseSwarmingNullMemberId() {
        Controller controllerNotNull = new Controller();
        MemberInfo memberInfo = new MemberInfo("One",null,"Three");
        Member member = new Member(controllerNotNull, memberInfo);
        assertEquals(false, Util.useSwarming(controllerNotNull, member));
        controllerNotNull.shutdown();
    }

    public void testUseSwarming() throws ConnectionException, IOException {

        File file = new File("build/test/first.config");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write("\n" +
                "net.bindaddress=127.0.0.1\n" +
                "random-port=false\n" +
                "net.port=3457\n" +
                "net.broadcast=false");

        writer.close();

        File newFile = new File("build/test/second.config");
        newFile.createNewFile();
        FileWriter newWriter = new FileWriter(newFile);
        newWriter.write("net.bindaddress=127.0.0.1");

        newWriter.close();

        Feature.P2P_REQUIRES_LOGIN_AT_SERVER.disable();

        Controller firstController = new Controller();
        Controller secondController = new Controller();
        firstController.startConfig("build/test/first.config");
        secondController.startConfig("build/test/second.config");

        firstController.connect(secondController.getConnectionListener().getAddress());
        Member member = firstController.getNodeManager().getConnectedNodes().iterator().next();
        assertTrue(Util.useSwarming(firstController, member));

        firstController.shutdown();
        secondController.shutdown();

        FileUtils.forceDelete(file);
        FileUtils.forceDelete(newFile);
    }

    public void testGetResourceUnableToFind() {
        assertNull(Util.getResource("asdasd","asdasd"));
    }

    public void testGetResourceFindFirst(){
        //Not null, resource is found without trying the alt location
        assertNotNull(Util.getResource(Constants.GETTING_STARTED_GUIDE_FILENAME,null));
    }

    public void testCopyResourceNull(){
        Path path = new File("/build/test").toPath();
        try {
            Util.copyResourceTo(null, "/etc", path, true, true);
            fail("Did not throw NullPointerException but resource was null");
        } catch (NullPointerException e){
            //OK since resource was null
        }
    }

    public void testCopyResourceNotFound() {
        Path path = new File("/build/test").toPath();
        assertNull(Util.copyResourceTo("asdas","asdasd", path, true, true));
    }

    public void testCopyResourceFileExistsNoOverwrite() throws IOException {
        //First, copy the file to make sure it's already there
        File source = new File("bin/" + Constants.GETTING_STARTED_GUIDE_FILENAME);
        File destination = new File("build/" + Constants.GETTING_STARTED_GUIDE_FILENAME);

        FileUtils.copyFile(source, destination);
        Path path = destination.toPath();

        Path returnedPath = Util.copyResourceTo(Constants.GETTING_STARTED_GUIDE_FILENAME, null, path, false, true);

        assertEquals(returnedPath, destination.toPath());

        //Cleanup
        FileUtils.forceDelete(destination);
    }

    public void testCopyResource() throws IOException {
        //Copy without overwriting
        Path destination = new File("build/" + Constants.GETTING_STARTED_GUIDE_FILENAME).toPath();
        Path returnedPath = Util.copyResourceTo(Constants.GETTING_STARTED_GUIDE_FILENAME, null, destination, false, true);

        assertTrue(destination.toFile().exists());

        //Cleanup
        FileUtils.forceDelete(destination.toFile());
    }

    public void testCopyResourceExceptionQuiet() {
        Path destination = new File("build").toPath();
        assertNull(Util.copyResourceTo(Constants.GETTING_STARTED_GUIDE_FILENAME, null, destination, false, true));

    }

    public void testCopyResourceExceptionNotQuiet() {
        Path destination = new File("build").toPath();
        assertNull(Util.copyResourceTo(Constants.GETTING_STARTED_GUIDE_FILENAME, null, destination, false, false));

    }

    public void testGetUrlContentNull() {
        URL url = null;
        try {
            Util.getURLContent(url);
            fail("NullPointer was not thrown when url was null");
        } catch (NullPointerException e){
            //Ok to throw null pointer
        }
    }

    public void testGetUrlFileDoesNotExist() throws MalformedURLException {
        File file = new File("build/test/testImage.png");
        //File does not exist so it returns null because IO Exception is thrown
        URL url = file.toURI().toURL();
        assertNull(Util.getURLContent(url));
    }

    public void testGetUrlOkFile() throws IOException {
        File file = new File("build/test/someFile.html");
        file.createNewFile();

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("This is a test string");
        fileWriter.close();

        URL url = file.toURI().toURL();
        assertEquals("This is a test string",Util.getURLContent(url));

        FileUtils.forceDelete(file);
    }

    public void testGetUrlSite() throws MalformedURLException {
        URL url = new URL("https://google.com");
        assertTrue(Util.getURLContent(url).contains("Google"));
    }

    public void testGetUrlNotInputStream() throws IOException {
        File file = new File("build/test/someFile.jpeg");
        file.createNewFile();

        URL url = file.toURI().toURL();
        assertNull(Util.getURLContent(url));

        FileUtils.forceDelete(file);
    }

    public void testSetClipboardContentsOk() throws IOException, UnsupportedFlavorException {

        Util.setClipboardContents("This is a test string");
        String valueFromKeyboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals("This is a test string", valueFromKeyboard);

        Util.setClipboardContents("!@#$%^&*()_+{}|'?>~/.,;'][");
        String specialCharactersFromClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals("!@#$%^&*()_+{}|'?>~/.,;'][", specialCharactersFromClipboard);

        Util.setClipboardContents("This \n is \n a \n multiline \n string");
        String multilineFromClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals("This \n is \n a \n multiline \n string", multilineFromClipboard);

        Util.setClipboardContents(null);
        String nullFromClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals(null, nullFromClipboard);

        Util.setClipboardContents("");
        String emptyStringFromClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals("", emptyStringFromClipboard);

        Util.setClipboardContents("   ");
        String spacesFromKeyboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        assertEquals("   ", spacesFromKeyboard);
    }

    public void testGetClipboardContentsOk() {

        StringSelection stringSelection = new StringSelection("This is some text");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
        assertEquals("This is some text", Util.getClipboardContents());

        StringSelection empty = new StringSelection("");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(empty, empty);
        assertEquals("", Util.getClipboardContents());

        StringSelection spaces = new StringSelection("    ");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(spaces, spaces);
        assertEquals("    ", Util.getClipboardContents());


    }

    public void testGetClipboardContentsNull() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //Creating a new transferable to return null, so that clipboard.getContents returns null
        clipboard.setContents(new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return null;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }

            @NotNull
            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return null;
            }
        }, new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {

            }
        });
        //Is equal to empty string because it was initialized in the getClipboardContents method
        assertEquals("", Util.getClipboardContents());

    }

    public void testGetClipboardContentsUnsupportedFlavor() throws IOException {
        BufferedImage image = new BufferedImage(1,2,3);

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return false;
            }

            @NotNull
            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return image;
            }
        }, new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {

            }
        });

        assertEquals("", Util.getClipboardContents());
    }

    public void testEndcodeForUrlOk(){
        assertEquals("%21%40%23%24%25%5E%26*", Util.endcodeForURL("!@#$%^&*"));
        assertEquals("test%2Ctesting", Util.endcodeForURL("test,testing"));
        assertEquals("ThisIsATest", Util.endcodeForURL("ThisIsATest"));
        assertEquals("+++", Util.endcodeForURL("   "));
        assertEquals("1234567", Util.endcodeForURL("1234567"));
    }

    public void testRemoveLastSlashFromUriNull() {
        assertNull(Util.removeLastSlashFromURI(null));
    }

    public void testRemoveLastSlashFromUriTrim() {
        assertEquals("http://powerfolder.com", Util.removeLastSlashFromURI("http://powerfolder.com   "));
        assertEquals("http://powerfolder.com", Util.removeLastSlashFromURI("     http://powerfolder.com"));
        assertEquals("http://powerfolder.com", Util.removeLastSlashFromURI("   http://powerfolder.com   "));
    }

    public void testRemoveLastSlashFromUriOk() {
        assertEquals("http://powerfolder.com", Util.removeLastSlashFromURI("http://powerfolder.com/"));
        assertEquals("http://powerfolder.com/", Util.removeLastSlashFromURI("http://powerfolder.com//"));
        assertEquals("", Util.removeLastSlashFromURI("/"));
        assertEquals("http://powerfolder.com", Util.removeLastSlashFromURI("http://powerfolder.com"));
        assertEquals("http://", Util.removeLastSlashFromURI("http://"));
    }

    public void testCompareIpAddressesNull(){
        try {
            Util.compareIpAddresses(null, null);
            fail("NullPointerException was not thrown when passing null arguments");
        } catch (NullPointerException e){
            //Supposed to throw NullPointerException
        }
    }

    public void testCompareIpAddressesSame() {
        byte[] firstAddress = {12, 23, 34, 45, 56, 67};
        byte[] sameValue = {12, 23, 34, 45, 56, 67};
        assertFalse(Util.compareIpAddresses(firstAddress, sameValue));

        byte[] passedReference = firstAddress;
        passedReference[0] = 0;
        assertFalse(Util.compareIpAddresses(firstAddress, passedReference));

        byte[] longAddress = new byte[100000];
        byte[] sameLongAddress = new byte[100000];
        for (int index = 0; index < 100000; index++) {
            longAddress[index] = (byte) index;
            sameLongAddress[index] = (byte) index;
        }

        byte[] someEmptyAddress = new byte[0];
        byte[] anotherEmptyAddress = new byte[0];

        assertFalse(Util.compareIpAddresses(someEmptyAddress, anotherEmptyAddress));

    }

    public void testCompareIpAddressesDifferent() {
        byte[] firstAddress = {12, 23, 34, 45, 56, 67};
        byte[] secondAddress = {12, 23, 34, 45, 56, 66};

        assertTrue(Util.compareIpAddresses(firstAddress, secondAddress));

        byte[] longAddress = new byte[100000];
        byte[] sameLongAddressModified = new byte[100000];
        for (int index = 0; index < 100000; index++) {
            longAddress[index] = (byte) index;
            sameLongAddressModified[index] = (byte) index;
        }
        sameLongAddressModified[99999] = (byte) (sameLongAddressModified[99999] - 1);
        assertTrue(Util.compareIpAddresses(sameLongAddressModified, longAddress));

        byte[] oneAddress = {1,2,3,5};
        byte[] shorterAddress = {1,2,3,4,5,6};
        assertFalse(Util.compareIpAddresses(oneAddress, shorterAddress));

        byte[] address = {1,2,3,4,5,6};
        byte[] addressShuffled = {1,3,2,4,6,5};
        assertTrue(Util.compareIpAddresses(address, addressShuffled));

    }

    public void testCompareIpSmaller(){
        byte[] firstIp = {1, 2, 3, 4, 5};
        byte[] secondIp = {1, 2, 3};

        //Will throw IndexOutOfBounds because second array is smaller in size
        assertFalse(Util.compareIpAddresses(firstIp, secondIp));
    }

    public void testSplitArraySizeBigger() {

        byte[] array = {1, 2, 3, 4, 5, 6, 7, 8};
        int size = 10;
        List<byte[]> oneChunk = Util.splitArray(array, size);
        assertEquals(1, oneChunk.size());
        assertEquals(array, oneChunk.get(0));

        byte[] empty = new byte[0];
        List<byte[]> emptyChunk = Util.splitArray(empty, size);
        assertEquals(1, oneChunk.size());
        assertEquals(empty, emptyChunk.get(0));

        byte[] veryBig = new byte[Integer.MAX_VALUE / 2];
        for (int index = 0; index < Integer.MAX_VALUE / 2; index++) {
            veryBig[index] = (byte) index;
        }

        int sizeMax = Integer.MAX_VALUE;
        List<byte[]> bigChunk = Util.splitArray(veryBig, sizeMax);
        assertEquals(1, bigChunk.size());
        assertEquals(veryBig, bigChunk.get(0));
    }

    public void testSplitArraySizeExceptions() {
        byte[] array = {1, 2, 3, 4, 5, 6, 7, 8};
        int size = 0;
        try {
            Util.splitArray(array, size);
            fail("Did not throw ArithmeticException but size was 0");
        } catch (ArithmeticException e){
            //OK since division by zero throws
        }

        size = -3;
        try {
            Util.splitArray(array, size);
            fail("Did not throw IllegalArgumentException but size was negative");
        } catch (IllegalArgumentException e){
            //OK since size was negative
        }
    }

    public void testSplitArraySizeDividesExactly() {
        byte[] array = {1, 2, 3, 4, 5, 6};
        int size = 2;
        List<byte[]> chunks = Util.splitArray(array, size);
        assertEquals(3, chunks.size());

        assertEquals(1,chunks.get(0)[0]);
        assertEquals(2,chunks.get(0)[1]);
        assertEquals(3,chunks.get(1)[0]);
        assertEquals(4,chunks.get(1)[1]);
        assertEquals(5,chunks.get(2)[0]);
        assertEquals(6,chunks.get(2)[1]);

        byte[] someArray = new byte[10000];
        for (int index = 0; index < 10000; index++) {
            someArray[index] = (byte) index;
        }
        int someSize = 10;
        List<byte[]> someChunks = Util.splitArray(someArray, someSize);
        assertEquals(10000 / 10, someChunks.size());
        for (int index = 0; index < someChunks.size(); index++) {
            assertEquals(someSize, someChunks.get(index).length);
        }
    }

    public void testSplitArrayLastChunkSize() {
        byte[] array = {1, 2, 3, 4, 5, 6, 7};
        int size = 2;
        List<byte[]> chunks = Util.splitArray(array, size);
        assertEquals(4, chunks.size());

        assertEquals(1,chunks.get(0)[0]);
        assertEquals(2,chunks.get(0)[1]);
        assertEquals(3,chunks.get(1)[0]);
        assertEquals(4,chunks.get(1)[1]);
        assertEquals(5,chunks.get(2)[0]);
        assertEquals(6,chunks.get(2)[1]);

        assertEquals(1, chunks.get(3).length);
        assertEquals(7, chunks.get(3)[0]);

        byte[] someArray = new byte[10009];
        for (int index = 0; index < 10009; index++) {
            someArray[index] = (byte) index;
        }
        int someSize = 10;
        List<byte[]> someChunks = Util.splitArray(someArray, someSize);
        assertEquals(10009 / 10 + 1, someChunks.size());
        for (int index = 0; index < 1000; index++) {
            assertEquals(someSize, someChunks.get(index).length);
        }
        assertEquals(9, someChunks.get(someChunks.size()-1).length);
    }

    public void testMergeArrayListNull() {
        List<byte[]> nullArray = null;
        try {
            Util.mergeArrayList(nullArray);
            fail("Did not throw NullPointerException for passing null array");
        } catch (NullPointerException e){
            //OK to throw since null list was passed
        }
    }

    public void testMergeArraySizeZero() {
        List<byte[]> empty = new ArrayList<>();
        byte[] result = Util.mergeArrayList(empty);
        assertEquals(0, result.length);
    }

    public void testMergeArrayListOk() {
        List<byte[]> listOfByteArrays = new ArrayList<>();
        for (int index = 0; index < 1000; index++) {
            listOfByteArrays.add(new byte[] {1,2,3,4});
        }

        byte[] result = Util.mergeArrayList(listOfByteArrays);
        assertEquals(1000 * 4, result.length);
        for (int index = 0; index < result.length; index++) {
            assertEquals(index % 4 + 1, result[index]);
        }

    }

    public void testParseConnectionNull() {
        String nullString = null;
        assertNull(Util.parseConnectionString(nullString));
    }

    public void testParseConnectionOk() {

        String addressWithPort = "localhost:8080";
        InetSocketAddress returnedAddress = Util.parseConnectionString(addressWithPort);
        assertEquals(returnedAddress.getHostName(), "localhost");
        assertEquals(8080, returnedAddress.getPort());

        String addressOnlyHost = "testing";
        InetSocketAddress returnedOnlyHost = Util.parseConnectionString(addressOnlyHost);
        assertEquals(returnedOnlyHost.getHostName(), "testing");
    }

    public void testParseConnectionColons() {
        //Exception is thrown and handled in method. Returns any local address
        String address = ":::::";
        InetSocketAddress returnedAddress = Util.parseConnectionString(address);
        assertEquals("localhost", returnedAddress.getHostName());
    }

    public void testParseConnectionOtherScenarios() {
        //Random local is returned
        String emptyAddress = "";
        InetSocketAddress returnedAddress = Util.parseConnectionString(emptyAddress);
        assertEquals("localhost", returnedAddress.getHostName());

        String address = "localhost:test";
        InetSocketAddress inetSocketAddress = Util.parseConnectionString(address);
        assertEquals("localhost" ,inetSocketAddress.getHostName());

    }

    public void testReplaceNotContains() {
        String mainString = "Mary had a little lamb";
        String whatToReplace = "apple";
        String replaceWith = "bananas";

        String returnedString = Util.replace(mainString, whatToReplace, replaceWith);
        assertEquals("Mary had a little lamb", returnedString);

        String emptyString = "";
        String returnedEmptyString = Util.replace(emptyString, whatToReplace, replaceWith);
        assertEquals("", returnedEmptyString);
    }

    public void testReplaceStringOk() {
        String mainString = "Is this the real life, is this just fantasy?";

        assertEquals("Is this the real life, is this just fantasy!!!", Util.replace(mainString, "?","!!!"));
        assertEquals("Is it the real life, is it just fantasy?", Util.replace(mainString, "this", "it"));
        assertEquals("Isthisthereallife,isthisjustfantasy?", Util.replace(mainString," ", ""));
        assertEquals(" is this just fantasy?", Util.replace(mainString,"Is this the real life,", ""));

    }

    public void testCreateHttpBuilderNullController() {
        Controller controller = null;
        try {
            Util.createHttpClientBuilder(controller);
            fail("NullPointerException was not thrown when controller was null");
        } catch (NullPointerException e){
            //OK since controller was null
        }
    }


    public void testCreateHttpBuilderProxyHost() throws IOException {
        File file = new File("build/test/first.config");
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write("\n" +
                "net.bindaddress=127.0.0.1\n" +
                "random-port=false\n" +
                "net.port=3457\n" +
                "net.broadcast=false\n" +
                "disableui=true");

        writer.close();
        Controller controllerLisa = Controller.createController();
        controllerLisa.startConfig("build/test/first.config");
        int statusCode = Util.createHttpClientBuilder(controllerLisa).build().execute(new HttpGet("http://powerfolder.com")).getStatusLine().getStatusCode();
        assertEquals(200, statusCode);

        controllerLisa.getConfig().put("security.ssl.trust_any","true");
        int statusCodeTrust = Util.createHttpClientBuilder(controllerLisa).build().execute(new HttpGet("http://powerfolder.com"))
                .getStatusLine().getStatusCode();

        assertEquals(200, statusCodeTrust);

        System.setProperty("http.proxyHost", "testProxy");
        System.setProperty("http.proxyPort", "1234");
        try {
            Util.createHttpClientBuilder(controllerLisa).build().execute(new HttpGet("http://powerfolder.com"));
            fail("Proxy was bad but it did not fail");
        } catch (ConnectException | UnknownHostException exception) {
            //OK Since proxy is bad
        }

        System.setProperty("http.proxyHost", "testProxy");
        System.setProperty("http.proxyPort", "1234");
        try {
            Util.createHttpClientBuilder(controllerLisa).build().execute(new HttpGet("http://powerfolder.com"));
            fail("Proxy was bad but it did not fail");
        } catch (ConnectException | UnknownHostException exception) {
            //OK Since proxy is bad
        }

        controllerLisa.getConfig().put("http.proxy.username","test");
        controllerLisa.getConfig().put("http.proxy.password","testpass");
        try {
            Util.createHttpClientBuilder(controllerLisa).build().execute(new HttpGet("http://powerfolder.com"));
            fail("Proxy was bad but it did not fail");
        } catch (ConnectException | UnknownHostException exception) {
            //OK Since proxy is bad
        }


        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");

        controllerLisa.shutdown();
    }

    public void testCompareVersions() {
        String firstVersion = "8.1.0 aaa";
        String secondVersion = "8.1.0 bbb";
        assertFalse(Util.compareVersions(secondVersion, firstVersion));

        String versionWeird = "TestOne";
        String anotherVersionWeird = "TestTwo";
        System.out.println(Util.compareVersions(versionWeird, anotherVersionWeird));
    }

    public void testBetweenVersions() {
        assertFalse(Util.betweenVersion("8.1.0", "8.0.0", "8.2.20"));
        assertTrue(Util.betweenVersion("8.1.0", "8.1.0", "8.2.20"));
        assertTrue(Util.betweenVersion("8.1.0", "8.2.11", "8.2.20"));
        assertFalse(Util.betweenVersion("8.1.0", "8.3.0", "8.2.20"));

        assertFalse(Util.betweenVersion("8.1.0", "8.2.1", "8.2.0"));
    }

    public void testMD5Multiple() {
        int n = 250000;
        for (int i = 0; i < n; i++) {
            testMD5();
        }
    }

    public void testMD5() {
        String magicId = IdGenerator.makeId();
        String id = IdGenerator.makeFolderId();

        // Do the magic...
        try {
            byte[] mId = magicId.getBytes("UTF-8");
            byte[] fId = id.getBytes("UTF-8");
            byte[] hexId = new byte[mId.length * 2 + fId.length];

            // Build secure ID base: [MAGIC_ID][FOLDER_ID][MAGIC_ID]
            System.arraycopy(mId, 0, hexId, 0, mId.length);
            System.arraycopy(fId, 0, hexId, mId.length - 1, fId.length);
            System.arraycopy(mId, 0, hexId, mId.length + fId.length - 2,
                    mId.length);
            new String(Util.encodeHex(Util.md5(hexId)));
        } catch (UnsupportedEncodingException e) {
            throw (IllegalStateException) new IllegalStateException(
                    "Fatal problem: UTF-8 encoding not found").initCause(e);
        }
    }

    public void testMySQLDeadlock() {
        assertFalse(Util.isMySQLDeadlock(new RuntimeException()));
        assertTrue(Util.isMySQLDeadlock(new RuntimeException("Problem while comitting to database. org.hibernate.TransactionException: JDBC commit failed com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: WSREP detected deadlock/conflict and aborted the transaction. Try restarting the transaction")));
        assertTrue(Util.isMySQLDeadlock(new RuntimeException("JDBC commit failed com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: WSREP detected deadlock/conflict and aborted the transaction. Try restarting the transaction")));
        assertTrue(Util.isMySQLDeadlock(new RuntimeException("WSREP detected deadlock/conflict and aborted the transaction. Try restarting the transaction")));
    }
}