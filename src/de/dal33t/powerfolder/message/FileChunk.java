/* $Id: FileChunk.java,v 1.6 2006/02/20 01:08:27 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Format;

/**
 * A file chunk, part of a upload / donwload
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FileChunk extends Message implements LimitBandwidth {
    private static final long serialVersionUID = 100L;

    public FileInfo file;
    public long offset;
    public byte[] data;

    public FileChunk() {
        // Serialisation constructor
    }

    public FileChunk(FileInfo file, long offset, byte[] data) {
        this.file = file;
        this.offset = offset;
        this.data = data;
    }

    public String toString() {
        return "FileChunk: " + file + " ("
            + Format.NUMBER_FORMATS.format(file.getSize())
            + " total bytes), offset: " + offset + ", chunk size: "
            + data.length;
    }
//
//    public void writeExternal(ObjectOutput out) throws IOException {
//        out.writeObject(file);
//        out.writeInt(data.length);
//        out.write(data);
//        //System.err.println("Wrote FileChunk. size " + data.length);
//    }
//
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        file = (FileInfo) in.readObject();
//        int size = in.readInt();
//        data = new byte[size];
//        in.read(data);
//        //System.err.println("Read FileChunk. size " + data.length);
//    }
}