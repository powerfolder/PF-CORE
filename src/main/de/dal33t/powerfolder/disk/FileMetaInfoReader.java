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
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * TODO SCHAASTER Add api doc
 */
public class FileMetaInfoReader extends PFComponent {

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
     * 
     * @param fInfo
     * @return
     */
    private FileInfo convertToMetaInfoFileInfo(Folder folder, FileInfo fInfo) {
        if (!(fInfo instanceof MP3FileInfo)
            && fInfo.getFilenameOnly().toUpperCase().endsWith(".MP3"))
        {
            if (logVerbose) {
                log().verbose("Converting to MP3 TAG: " + fInfo);
            }
            // Not an mp3 fileinfo ? convert !
            File diskFile = fInfo.getDiskFile(getController()
                .getFolderRepository());
            // Create mp3 fileinfo
            MP3FileInfo mp3FileInfo = new MP3FileInfo(folder, diskFile);
            mp3FileInfo.copyFrom(fInfo);
            return mp3FileInfo;
        }

        if (!(fInfo instanceof ImageFileInfo) && UIUtil.isAWTAvailable()
            && ImageSupport.isReadSupportedImage(fInfo.getFilenameOnly()))
        {
            if (logVerbose) {
                log().verbose("Converting to Image: " + fInfo);
            }
            File diskFile = fInfo.getDiskFile(getController()
                .getFolderRepository());
            // Create image fileinfo
            ImageFileInfo imageFileInfo = new ImageFileInfo(folder, diskFile);
            imageFileInfo.copyFrom(fInfo);
            return imageFileInfo;
        }
        // Otherwise file is correct
        return fInfo;
    }

}
