package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FileListRequestProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message to re-request the file list from another member.
 *
 * @author Sprajc
 */
public class FileListRequest extends FolderRelatedMessage
  implements D2DObject
{
    private static final long serialVersionUID = 100L;

    public FileListRequest(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        folder = foInfo;
    }

    @Override
    public String toString() {
        return "FileListRequest [folder=" + folder.getLocalizedName() + "/" + folder.id + "]";
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FileListRequestProto.FileListRequest)
        {
          FileListRequestProto.FileListRequest proto =
            (FileListRequestProto.FileListRequest)mesg;

          this.folder = new FolderInfo(proto.getFolderInfo());
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FileListRequestProto.FileListRequest.Builder builder =
        FileListRequestProto.FileListRequest.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      builder.setFolderInfo((FolderInfoProto.FolderInfo)this.folder.toD2D());

      return builder.build();
    }
}
