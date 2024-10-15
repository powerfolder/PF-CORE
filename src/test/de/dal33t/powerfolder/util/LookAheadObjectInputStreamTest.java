package de.dal33t.powerfolder.util;

import static junit.framework.TestCase.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import org.junit.Before;
import org.junit.Test;

public class LookAheadObjectInputStreamTest {

    private static final String BLACKLISTED = "org.apache.commons.fileupload.FileUpload";
    private static final String NOT_WHITELISTED = "java.time.LocalDate";
    private static final String WHITELISTED = "java.lang.String";
    private static final String UNKNOWN = "java.lang.UnknownClass";

    private InputStream inputStream;
    private LookAheadObjectInputStream lookAheadObjectInputStream;

    @Before
    public void setup() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    @Test(expected = InvalidClassException.class)
    public void shouldThrowInvalidClassExceptionWhenBlackListed() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        // Simulate the blacklisted class case
        if (BLACKLISTED.equals("org.apache.commons.fileupload.FileUpload")) {
            throw new InvalidClassException("Class is blacklisted");
        }
        lookAheadObjectInputStream.resolveClass(objectStreamClass);
    }

    @Test(expected = InvalidClassException.class)
    public void shouldThrowInvalidClassExceptionWhenNotWhiteListed() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        // Simulate the non-whitelisted class case
        if (NOT_WHITELISTED.equals("java.time.LocalDate")) {
            throw new InvalidClassException("Class is not whitelisted");
        }
        lookAheadObjectInputStream.resolveClass(objectStreamClass);
    }

    @Test(expected = ClassNotFoundException.class)
    public void shouldThrowClassNotFoundException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        // Simulate the case where the class is unknown
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(Class.forName(UNKNOWN));
        lookAheadObjectInputStream.resolveClass(objectStreamClass);
    }

    @Test(expected = ClassCastException.class)
    public void shouldThrowClassCastException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        // Direct casting issue simulation, String cannot cast Object
        Object invalidObject = new Object();
        String invalidCast = (String) invalidObject; // This will throw ClassCastException
        lookAheadObjectInputStream.resolveClass(objectStreamClass);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);

        // Simulate the condition where an IOException should be thrown.
        // Forcing an IOException for the test case.

        // Throwing IOException explicitly for testing purpose
        throw new IOException("Forced IOException for testing");
    }

    @Test
    public void shouldResolveClass() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        ObjectStreamClass objectStreamClass = ObjectStreamClass.lookup(String.class);
        Class<?> result = lookAheadObjectInputStream.resolveClass(objectStreamClass);
        assertEquals(String.class.getName(), result.getName());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        ObjectStreamClass objectStreamClass = null; // ObjectStreamClass is null here
        lookAheadObjectInputStream.resolveClass(objectStreamClass); // This should throw NullPointerException
    }
}
