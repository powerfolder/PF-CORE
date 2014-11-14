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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;

import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Helper class which serializes and deserializes java objects into byte arrays
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class ByteSerializer extends Loggable {
    private static final Logger LOG = Logger.getLogger(ByteSerializer.class
        .getName());

    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024;
    // Should at least cover one file chunk. if packet is greater, the buffer
    // won't get cached = memory waste.
    private static final int MAX_CACHE_BUFFER_SIZE = 0;

    private static final boolean CACHE_OUT_BUFFER = false;

    private Reference<ByteArrayOutputStream> outBufferRef;
    private Reference<byte[]> inBufferRef;

    public static boolean BENCHMARK = false;
    private static final Map<Class<?>, Integer> CLASS_STATS = new ConcurrentHashMap<Class<?>, Integer>();
    private static long totalTime = 0;
    private static int totalObjects = 0;

    public ByteSerializer() {
    }

    /**
     * Serialize an object. This method is non-static an re-uses the internal
     * byteoutputstream
     *
     * @param target
     *            The object to be serialized
     * @param compress
     *            true if serialization should compress.
     * @param padToSize
     *            the size to pad the output buffer to. number below 0 means no
     *            padding.
     * @return The serialized object
     * @throws IOException
     *             In case the object cannot be serialized
     */
    public byte[] serialize(Serializable target, boolean compress, int padToSize)
        throws IOException
    {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream byteOut;
        // Reset buffer
        if (outBufferRef != null && outBufferRef.get() != null) {
            // Reuse old buffer
            byteOut = outBufferRef.get();
            byteOut.reset();
        } else {
            // logFiner("Creating send buffer (512bytes)");
            // Create new bytearray output, 512b buffer
            byteOut = new ByteArrayOutputStream(512);
            if (CACHE_OUT_BUFFER) {
                // Chache outgoing buffer
                outBufferRef = new SoftReference<ByteArrayOutputStream>(byteOut);
            }
        }

        OutputStream targetOut;
        // Serialize....
        if (compress) {
            PFZIPOutputStream zipOut = new PFZIPOutputStream(byteOut);
            targetOut = zipOut;
        } else {
            targetOut = byteOut;
        }
        ObjectOutputStream objOut = new ObjectOutputStream(targetOut);

        // Write
        try {
            objOut.writeUnshared(target);
        } catch (StreamCorruptedException e) {
            LOG.log(Level.WARNING, "Problem while serializing: " + e, e);
            throw e;
        } catch (InvalidClassException e) {
            LOG.log(Level.WARNING, "Problem while serializing: " + target
                + ": " + e, e);
            throw e;
        }

        objOut.close();

        if (padToSize > 0) {
            int modulo = byteOut.size() % padToSize;
            if (modulo != 0) {
                int additionalBytesRequired = padToSize - (modulo);
                // LOG.warn("Buffersize: " + byteOut.size()
                // + ", Additonal bytes required: " + additionalBytesRequired);
                for (int i = 0; i < additionalBytesRequired; i++) {
                    byteOut.write(0);
                }
            }
        }
        byteOut.flush();
        byteOut.close();

        if (byteOut.size() >= 256 * 1024) {
            logWarning("Send buffer exceeds 256KB! "
                + Format.formatBytes(byteOut.size()) + ". Message: " + target);
        }

        byte[] buf = byteOut.toByteArray();
        if (BENCHMARK) {
            totalObjects++;
            totalTime += System.currentTimeMillis() - start;
            int count = 0;
            if (CLASS_STATS.containsKey(target.getClass())) {
                count = CLASS_STATS.get(target.getClass());
            }
            count++;
            CLASS_STATS.put(target.getClass(), count);
        }
        return buf;
    }

    /**
     * Re-uses internal received buffer for incoming readings.
     *
     * @param in
     *            the input stream to deserialize from
     * @param expectedSize
     *            the expected size
     * @throws IOException
     * @return the deserialized byte array. the array might be bigger than
     *         expected size.
     */
    public byte[] read(InputStream in, int expectedSize) throws IOException {
        if (expectedSize > MAX_BUFFER_SIZE) {
            throw new IOException(
                "Max buffersize overflow while reading. expected size "
                    + expectedSize);
        }
        byte[] byteIn = null;

        // Dont cache buffer
        if (expectedSize > MAX_CACHE_BUFFER_SIZE) {
            // if (isFiner()) {
            // logFiner("Uncached buffer: " + expectedSize);
            // }
            byteIn = new byte[expectedSize];
            // Read into receivebuffer
            StreamUtils.read(in, byteIn, 0, expectedSize);
            return byteIn;
        }

        // Resolve old cache
        if (inBufferRef != null && inBufferRef.get() != null) {
            // Re-use old buffer
            byteIn = inBufferRef.get();
        }

        // Check buffer
        if (byteIn == null || byteIn.length < expectedSize) {
            if (isFiner()) {
                String action = (byteIn == null) ? "Creating" : "Extending";
                logFiner(action + " receive buffer ("
                    + Format.formatBytes(expectedSize) + ")");
            }
            if (expectedSize >= 128 * 1024) {
                logWarning("Recived buffer exceeds 128KB! "
                    + Format.formatBytes(expectedSize));
            }
            byteIn = new byte[expectedSize];
            // Cache buffer
            inBufferRef = new SoftReference<byte[]>(byteIn);
        }

        // Read into receivebuffer
        StreamUtils.read(in, byteIn, 0, expectedSize);

        return byteIn;
    }

    // Static serialization ***************************************************

    /**
     * Serialize an object
     *
     * @param target
     *            The object to be serialized
     * @param compress
     *            true if the stream should be compressed.
     * @return The serialized object
     * @throws IOException
     *             In case the object cannot be serialized
     */
    public static byte[] serializeStatic(Serializable target, boolean compress)
        throws IOException
    {
        return new ByteSerializer().serialize(target, compress, -1);
    }

    /**
     * Deserialize a byte[] array into an Object.
     * 
     * @param base
     *            The byte[] array
     * @param expectCompression
     *            if there is a zip compression expected
     * @return The deserialized object
     * @throws IOException
     *             an I/O Error occured
     * @throws ClassNotFoundException
     *             the class for the Object to be deserialized cannot be found.
     */
    public static Object deserializeStatic(byte[] base,
        boolean expectCompression) throws IOException, ClassNotFoundException
    {
        Object result;
        try {
            result = deserialize0(base, expectCompression);
        } catch (IOException e) {
            try {
                result = deserialize0(base, !expectCompression);
            } catch (StreamCorruptedException e2) {
                LOG.log(Level.WARNING, "While deserializing "
                    + (expectCompression ? "   compressed" : "uncompressed")
                    + ": " + e, e);
                if (!e2.toString().toLowerCase()
                    .contains("invalid stream header: 1f8b0800"))
                {
                    LOG.log(Level.WARNING, "While deserializing "
                        + (!expectCompression
                            ? "   compressed"
                            : "uncompressed") + ": " + e2, e2);
                }
                throw e2;
            } catch (InvalidClassException e2) {
                LOG.log(Level.WARNING, "While deserializing: " + e2, e2);
                throw e2;
            }
            if (!(result instanceof Identity)) {
                LOG.warning("Stream was not as expected ("
                    + (expectCompression
                        ? "compression was expected, but received uncompressed data"
                        : "no compression was expected, but received compressed data")
                    + ") on " + result);
            }

        }
        return result;
    }

    /**
     * Deserializer method with flag indicating if the base array is zip
     * compressed
     *
     * @param base
     * @param compressed
     * @return the dezerialized object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static Object deserialize0(byte[] base, boolean compressed)
        throws IOException, ClassNotFoundException
    {
        long start = System.currentTimeMillis();
        ObjectInputStream in = null;
        Object result = null;
        try {
            InputStream targetIn;
            // deserialize from the array.......u
            ByteArrayInputStream bin = new ByteArrayInputStream(base);
            if (compressed) {
                GZIPInputStream zipIn = new GZIPInputStream(bin);
                targetIn = zipIn;
            } else {
                targetIn = bin;
            }
            in = new ObjectInputStream(targetIn);
            result = in.readUnshared();
            return result;
        } finally {
            if (in != null) {
                in.close();
            }
            if (BENCHMARK && result != null) {
                totalObjects++;
                totalTime += System.currentTimeMillis() - start;

                int count = 0;
                if (CLASS_STATS.containsKey(result.getClass())) {
                    count = CLASS_STATS.get(result.getClass());
                }
                count++;
                CLASS_STATS.put(result.getClass(), count);
            }
        }
    }

    // Helper code ************************************************************

    public static final void printStats() {
        if (totalObjects != 0) {
            LOG.fine("Serialization perfomance: " + totalObjects + " took "
                + totalTime + "ms. That is "
                + ((totalTime * 1000) / totalObjects)
                + " ms/1k objects. Message stats: ");
            List<Class<?>> sorted = new ArrayList<Class<?>>(CLASS_STATS
                .keySet());
            Collections.sort(sorted, new Comparator<Class<?>>() {
                public int compare(Class<?> o1, Class<?> o2) {
                    Integer n1 = CLASS_STATS.get(o1);
                    Integer n2 = CLASS_STATS.get(o2);
                    if (n2 == null) {
                        return -1;
                    }
                    return n2.compareTo(n1);
                }
            });
            for (Class<?> clazz : sorted) {
                LOG.fine(" " + clazz.getName() + ": " + CLASS_STATS.get(clazz));
            }
        } else {
            LOG.fine("Serialization perfomance: " + totalObjects);
        }
    }
}