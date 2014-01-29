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
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), size);
        writeHeader();
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            // Workaround for: PFS-1172: http://bugs.java.com/view_bug.do?bug_id=4797189
            if (def != null) {
                def.end();                
            }
        } catch (Exception e) {
        }
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

    // /**
    // * Resets the stream and writes a new header
    // *
    // * @throws IOException
    // */
    // private synchronized void reset() throws IOException {
    // def = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    // buf = new byte[512];
    // writeHeader();
    // }

    /**
     * Writes a new header
     * 
     * @throws IOException
     */
    private synchronized void writeHeader() throws IOException {
        out.write(header);
    }
}