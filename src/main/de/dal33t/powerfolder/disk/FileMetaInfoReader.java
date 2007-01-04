package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.ImageFileInfo;
import de.dal33t.powerfolder.light.MP3FileInfo;
import de.dal33t.powerfolder.util.ImageSupport;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * TODO SCHAASTER Add api doc
 */
public class FileMetaInfoReader extends PFComponent {

    private static final Logger LOG = Logger
        .getLogger(FileMetaInfoReader.class);

    public FileMetaInfoReader(Controller controller) {
        super(controller);
    }

    public static boolean isConvertingSupported(FileInfo fileInfo) {
        return fileInfo.getFilenameOnly().toUpperCase().endsWith(".MP3")
            || (UIUtil.isAWTAvailable() && ImageSupport
                .isReadSupportedImage(fileInfo.getFilenameOnly()));

    }

    public List<FileInfo> convert(Folder folder, List<FileInfo> filesToConvert)
    {
        List<FileInfo> converted = new ArrayList<FileInfo>(filesToConvert
            .size());
        for (FileInfo fileInfo : filesToConvert) {
            // Only load metadata if file is not ignored
            if (!folder.getBlacklist().isIgnored(fileInfo)) {
                converted.add(convertToMetaInfoFileInfo(folder, fileInfo));
            }
        }
        return converted;
    }

    /**
     * Converts a fileinfo into a more detailed meta info loaded fileinfo. e.g.
     * with mp3 tags
     * <p>
     * TODO Check places where this is called!
     * 
     * @param fInfo
     * @return
     */
    public static FileInfo convertToMetaInfoFileInfo(Folder folder,
        FileInfo fInfo)
    {
        if (!(fInfo instanceof MP3FileInfo)
            && fInfo.getFilenameOnly().toUpperCase().endsWith(".MP3"))
        {
            if (LOG.isVerbose()) {
                LOG.verbose("Converting to MP3 TAG: " + fInfo);
            }
            // Not an mp3 fileinfo ? convert !
            File diskFile = fInfo.getDiskFile(folder.getController()
                .getFolderRepository());
            // Create mp3 fileinfo
            MP3FileInfo mp3FileInfo = new MP3FileInfo(folder, diskFile);
            mp3FileInfo.copyFrom(fInfo);
            return mp3FileInfo;
        }

        // Disabled image reading. Very slow and bring few infos
//        if (!(fInfo instanceof ImageFileInfo) && UIUtil.isAWTAvailable()
//            && ImageSupport.isReadSupportedImage(fInfo.getFilenameOnly()))
//        {
//            if (LOG.isVerbose()) {
//                LOG.verbose("Converting to Image: " + fInfo);
//            }
//            File diskFile = fInfo.getDiskFile(folder.getController()
//                .getFolderRepository());
//            // Create image fileinfo
//            ImageFileInfo imageFileInfo = new ImageFileInfo(folder, diskFile);
//            imageFileInfo.copyFrom(fInfo);
//            return imageFileInfo;
//        }
        // Otherwise file is correct
        return fInfo;
    }

}
