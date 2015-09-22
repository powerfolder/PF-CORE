package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;

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
  implements D2DMessage
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
    initFromD2DMessage(AbstractMessage mesg)
    {
      if(mesg instanceof FileListRequestProto.FileListRequest)
        {
          FileListRequestProto.FileListRequest proto =
            (FileListRequestProto.FileListRequest)mesg;

          this.folder = new FolderInfo(proto.getFolder());
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2DMessage()
    {
      FileListRequestProto.FileListRequest.Builder builder =
        FileListRequestProto.FileListRequest.newBuilder();

      builder.setClassName("FileListRequest");
      builder.setFolder((FolderInfoProto.FolderInfo)this.folder.toD2DMessage());

      return builder.build();
    }
}
