package de.dal33t.powerfolder.util;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private static final String UNKNOWN = "java.lang.NotAClass";
    private ObjectStreamClass mockedObjectStreamClass = mock(ObjectStreamClass.class);
    private InputStream inputStream ;
    private LookAheadObjectInputStream lookAheadObjectInputStream;

    @Before
    public void setup() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream =new ObjectOutputStream(byteArrayOutputStream);
        inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    @Test(expected = InvalidClassException.class)
    public void shouldThrowInvalidClassExceptionWhenBlackListed() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        when(mockedObjectStreamClass.getName()).thenReturn(BLACKLISTED);
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);

    }
    @Test(expected = InvalidClassException.class)
    public void shouldThrowInvalidClassExceptionWhenNotWhiteListed() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        when(mockedObjectStreamClass.getName()).thenReturn(NOT_WHITELISTED);
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);

    }

    @Test(expected = ClassNotFoundException.class)
    public void shouldThrowClassNotFoundException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        when(mockedObjectStreamClass.getName()).thenReturn(UNKNOWN);
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);
    }


    @Test(expected = ClassCastException.class)
    public void shouldThrowClassCastException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        String invalidObject =(String) new Object();
        when(mockedObjectStreamClass.getName()).thenReturn(invalidObject);
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        when(mockedObjectStreamClass.getName()).thenReturn("");
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);
    }

    @Test
    public void shouldResolveClass() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        when(mockedObjectStreamClass.getName()).thenReturn(WHITELISTED);
        Class<?> result = lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);
        assertEquals(String.class.getName(),result.getName());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerException() throws IOException, ClassNotFoundException {
        lookAheadObjectInputStream = new LookAheadObjectInputStream(inputStream);
        String nullObj = null;
        when(mockedObjectStreamClass.getName()).thenReturn(nullObj);
        Class<?> result =lookAheadObjectInputStream.resolveClass(mockedObjectStreamClass);
    }
}
