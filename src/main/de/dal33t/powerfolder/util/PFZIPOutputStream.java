/* $Id: PFZIPOutputStream.java,v 1.1 2005/03/28 15:40:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A GZipoutput stream which is resettable and does not need new buffer
 * allocation. Also no trailer writing code included
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class PFZIPOutputStream extends DeflaterOutputStream {

    private final static int GZIP_MAGIC = 0x8b1f;

    /**
     * Creates a new output stream with the specified buffer size.
     * 
     * @param out
     *            the output stream
     * @param size
     *            the output buffer size
     * @exception IOException
     *                If an I/O error has occurred.
     * @exception IllegalArgumentException
     *                if size is <= 0
     */
    public PFZIPOutputStream(OutputStream out, int size) throws IOException {
        super(out, new Deflater(Deflater.BEST_COMPRESSION, true), size);
        reset();
    }

    /**
     * Creates a new output stream with a default buffer size.
     * 
     * @param out
     *            the output stream
     * @exception IOException
     *                If an I/O error has occurred.
     */
    public PFZIPOutputStream(OutputStream out) throws IOException {
        this(out, 512);
    }

    private final static byte[] header = {(byte) GZIP_MAGIC,
        (byte) (GZIP_MAGIC >> 8), Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0};

    /**
     * Resets the stream and writes a new header
     * @throws IOException
     */
    public synchronized void reset() throws IOException {
        def = new Deflater(Deflater.BEST_COMPRESSION, true);
        buf = new byte[512];
        out.write(header);
    }
}