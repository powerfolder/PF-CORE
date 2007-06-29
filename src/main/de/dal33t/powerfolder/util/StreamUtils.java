package de.dal33t.powerfolder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class containing utility methods when working with streams.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class StreamUtils {
    private static final Logger LOG = Logger.getLogger(StreamUtils.class);

    private static final int BUFFER_SIZE = 1024;

    private StreamUtils() {
    }

    /**
     * Copies the file to the destination stream.
     * 
     * @param source
     * @param destination
     * @throws IOException
     */
    public static void copyToStream(File source, OutputStream destination)
        throws IOException
    {

        Reject.ifNull(source, "Source is null");
        Reject.ifNull(destination, "Destination is null");
        Reject.ifNull(source.exists(), "Source file does not exist");
        Reject.ifNull(source.canRead(), "Unable to read source file");

        FileInputStream in = new FileInputStream(source);
        copyToStream(in, destination);
        in.close();
    }

    /**
     * Copies the content stream into the output stream unless no more data is
     * availble. Uses internal buffer to speed up operation.
     * 
     * @param source
     * @param destination
     * @throws IOException
     */
    public static void copyToStream(InputStream source, OutputStream destination)
        throws IOException
    {
        copyToStream(source, destination, -1);
    }

    /**
     * Copies the content stream into the output stream unless no more data is
     * availble. Uses internal buffer to speed up operation.
     * 
     * @param source
     * @param destination
     * @param bytesToTransfer
     *            the bytes to transfer.
     * @throws IOException
     */
    public static void copyToStream(InputStream source,
        OutputStream destination, long bytesToTransfer) throws IOException
    {
        Reject.ifNull(source, "Source is null");
        Reject.ifNull(destination, "Destination is null");

        if (source.available() <= 0) {
            return;
        }

        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        long totalRead = 0;
        while (true) {
            len = source.read(buf);
            if (len < 0) {
                break;
            }
            totalRead += len;
            destination.write(buf, 0, len);
        }
    }

    /**
     * Reads a specific amout of data from a stream. Wait util enough data is
     * available
     * 
     * @param in
     *            the inputstream
     * @param buffer
     *            the buffer to put in the data
     * @param offset
     *            the start offset in the buffer
     * @param size
     *            the number of bytes to read
     * @throws IOException
     *             if stream error
     */
    public static void read(InputStream in, byte[] buffer, int offset,
        int size) throws IOException
    {
        int nTotalRead = 0;
        int nRead = 0;
        do {
            try {
                nRead = in.read(buffer, offset + nTotalRead, size
                    - nTotalRead);
            } catch (IndexOutOfBoundsException e) {
                LOG.error("buffer.lenght: " + buffer.length + ", offset");
                throw e;
            }
            if (nRead < 0) {
                throw new IOException("EOF, nothing more to read");
            }
            nTotalRead += nRead;
        } while (nTotalRead < size);
    }
}
