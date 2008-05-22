package de.dal33t.powerfolder.ui.render;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.ImageFileInfo;
import de.dal33t.powerfolder.light.MP3FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.folder.DirectoryTableModel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Renders a Directory, FileInfo or Status line for the DirectoryTable.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.18 $
 */
public class DirectoryTableCellRenderer extends DefaultTableCellRenderer {
    private Controller controller;
    private DirectoryTableModel tableModel;
    private final Color AVAILEBLE = Color.GRAY;
    private final Color DOWNLOADING = new Color(40, 170, 40);
    private final Color DELETED = Color.RED;
    private final Color NORMAL = Color.BLACK;
    private final Color NEWER_AVAILABLE = Color.BLUE;
    private final Color STATUS = Color.GRAY;

    /**
     * Initalizes a FileTableCellrenderer upon a <code>FileListTableModel</code>
     * 
     * @param controller
     *            the controller
     * @param tableModel
     *            the table model to act on
     */
    public DirectoryTableCellRenderer(Controller controller,
        DirectoryTableModel tableModel)
    {
        this.controller = controller;
        this.tableModel = tableModel;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        setIcon(null);
        setToolTipText(null);
        Object fileDirOrStatus = value;
        int columnInModel = UIUtil.toModel(table, column);
        if (fileDirOrStatus instanceof FileInfo) {
            return render((FileInfo) fileDirOrStatus, columnInModel, table,
                isSelected, hasFocus, row, column);
        } else if (fileDirOrStatus instanceof Directory) {
            return render((Directory) fileDirOrStatus, columnInModel, table,
                isSelected, hasFocus, row, column);
        } else if (fileDirOrStatus instanceof String) {
            return render((String) fileDirOrStatus, columnInModel, table,
                isSelected, hasFocus, row, column);
        }
        throw new IllegalStateException(
            "expected FileInfo, Directory or String not: "
                + fileDirOrStatus.getClass().getName());

    }

    /** renders the status line if folder empty */
    private Component render(String statusLine, int columnInModel,
        JTable table, boolean isSelected, boolean hasFocus, int row, int column)
    {
        if (columnInModel == 1) {
            // filename column is the most visible so insert status line there..
            setForeground(STATUS);
            setHorizontalAlignment(SwingConstants.LEFT);
            setToolTipText(statusLine);
            return super.getTableCellRendererComponent(table, statusLine,
                isSelected, hasFocus, row, column);
        }
        // no text in other columns
        return super.getTableCellRendererComponent(table, "", isSelected,
            hasFocus, row, column);

    }

    private Component render(FileInfo fileInfo, int columnInModel,
        JTable table, boolean isSelected, boolean hasFocus, int row, int column)
    {
        String newValue = "";
        switch (columnInModel) {
            case 0 : { // file type
                Icon icon = Icons.getIconFor(fileInfo, controller);
                setIcon(icon);
                setHorizontalAlignment(SwingConstants.LEFT);
                break;
            }
            case 1 : {// filename

                if (tableModel.isRecursive()) {
                    newValue = fileInfo.getName();
                } else {
                    newValue = fileInfo.getFilenameOnly();
                }
                // Prepare diskfile
                File diskFile = fileInfo.getDiskFile(controller
                    .getFolderRepository());
                if (diskFile != null) {
                    if (!diskFile.exists()) {
                        diskFile = null;
                    }
                }
                String fileNameForTooltip;
                if (diskFile != null) {
                    fileNameForTooltip = replaceSpacesWithNBSP(diskFile
                        .getAbsolutePath());
                } else {
                    fileNameForTooltip = replaceSpacesWithNBSP(newValue);
                }
                render0(fileInfo, fileNameForTooltip);
                setHorizontalAlignment(SwingConstants.LEFT);
                break;
            }
            case 2 : { // file size
                newValue = Format.formatBytesShort(fileInfo.getSize());
                setToolTipText(fileInfo.getSize() + "");
                setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            }
            case 3 : { // member nick
                MemberInfo member = fileInfo.getModifiedBy();
                newValue = member.nick;
                setIcon(Icons.getSimpleIconFor(member.getNode(controller)));
                setHorizontalAlignment(SwingConstants.LEFT);
                break;
            }
            case 4 : {// modified date
                newValue = Format.formatDate(fileInfo.getModifiedDate());
                setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            }
            case 5 : { // availability
                Folder folder = controller.getFolderRepository().getFolder(
                    fileInfo.getFolderInfo());
                FileInfoHolder holder = folder.getDirectory()
                    .getFileInfoHolder(fileInfo);
                if (holder == null) {
                    newValue = "0";
                } else {
                    newValue = holder.getAvailability() + "";
                    List<Member> members = holder.getSources();
                    String toolTipValue = "<HTML><BODY>";
                    String separetor = "";
                    for (int i = 0; i < members.size(); i++) {
                        toolTipValue += separetor + members.get(i).getNick();
                        separetor = "<BR>";
                    }
                    // insert filename to force redraw of tooltip on new file
                    // with same members
                    toolTipValue += "<!-- " + fileInfo.getFilenameOnly()
                        + "--></BODY></HTML>";
                    setToolTipText(toolTipValue);
                }
            }
        }
        return super.getTableCellRendererComponent(table, newValue, isSelected,
            hasFocus, row, column);
    }

    private Component render(Directory directory, int columnInModel,
        JTable table, boolean isSelected, boolean hasFocus, int row, int column)
    {
        setIcon(null);
        String newValue = "";
        switch (columnInModel) {
            case 0 : { // file type
                setIcon(Icons.getIconFor(directory, false, controller));
                setHorizontalAlignment(LEFT);
                break;
            }
            case 1 : {// filename
                newValue = directory.getName();
                // preference goes to deleted, then ignored then available icon
                if (directory.isDeleted())
                {
                    setForeground(DELETED);
                    setIcon(Icons.DELETE);
                } else if (directory.isRetained() &&
                        directory.isFolderWhitelist())
                {
                    setIcon(Icons.WHITE_LIST);
                    setForeground(NORMAL);
                } else if (!directory.isRetained() &&
                        !directory.isFolderWhitelist())
                {
                    setIcon(Icons.BLACK_LIST);
                    setForeground(NORMAL);
                } else if (directory.isExpected(controller
                    .getFolderRepository()))
                {
                    setForeground(AVAILEBLE);
                    setIcon(Icons.EXPECTED);
                } else {
                    setForeground(NORMAL);
                }
                setToolTipText(newValue);
                setHorizontalAlignment(LEFT);
                break;
            }
            case 2 : { // file size
                break;
            }
            case 3 : { // member nick
                break;
            }
            case 4 : {// modified date
                break;
            }
            case 5 : { // availability
                break;
            }
        }

        return super.getTableCellRendererComponent(table, newValue, isSelected,
            hasFocus, row, column);
    }

    private final void render0(FileInfo fInfo, String fileNameForTooltip) {

        // Obtain the newest version of this file
        FileInfo newestVersion = null;
        FileInfo newestDeletedVersion = null;
        Folder folder = controller.getFolderRepository().getFolder(
            fInfo.getFolderInfo());
        if (fInfo.getFolder(controller.getFolderRepository()) != null) {
            newestVersion = fInfo.getNewestNotDeletedVersion(controller
                .getFolderRepository());
            newestDeletedVersion = fInfo.getNewestVersion(controller
                .getFolderRepository());
        }
        setIcon(null);

        String statusForTooltip = null;
        if (fInfo.isDownloading(controller)) {
            setForeground(DOWNLOADING);
            DownloadManager dl = controller.getTransferManager().getActiveDownload(
                fInfo);
            if (dl != null && dl.isStarted()) {
                // FIXME: !!
                StringBuilder b = new StringBuilder();
                for (Download d: dl.getSources()) {
                    if (b.length() > 0) {
                        b.append(", ");
                    }
                    b.append(d.getPartner().getNick());
                }
                setIcon(Icons.DOWNLOAD_ACTIVE);
                statusForTooltip = Translation.getTranslation(
                    "fileinfo.downloading_from_member", b.toString());
            } else {
                setIcon(Icons.DOWNLOAD);
                statusForTooltip = Translation
                    .getTranslation("transfers.queued");
            }
        // preference goes to deleted, then ignored then available icon
        } else if (fInfo.isDeleted()) {
            setForeground(DELETED);
            setIcon(Icons.DELETE);
            statusForTooltip = Translation.getTranslation("fileinfo.deleted");

        } else if (folder.getDiskItemFilter().isExcluded(fInfo) &&
                !folder.isWhitelist()) {
            // Blacklist and file filtered out by blacklist.
            setIcon(Icons.BLACK_LIST);
            statusForTooltip = replaceSpacesWithNBSP(Translation
                    .getTranslation("fileinfo.ignore"));
            setForeground(NORMAL);
        } else if (!folder.getDiskItemFilter().isExcluded(fInfo) &&
                folder.isWhitelist()) {
            // Whitelist and file not filtered out by whitelist.
            setIcon(Icons.WHITE_LIST);
            statusForTooltip = replaceSpacesWithNBSP(Translation
                    .getTranslation("fileinfo.ignore"));
            setForeground(NORMAL);
        } else if (fInfo.isExpected(controller.getFolderRepository())) {
            setForeground(AVAILEBLE);

            setIcon(Icons.EXPECTED);
            statusForTooltip = Translation.getTranslation("fileinfo.expected");

        } else if (newestVersion != null && newestVersion.isNewerThan(fInfo)) {
            // A newer version is available
            // FIXME: If newest version (e.g. v10) is deleted, but a
            // newer (e.g. v9) is available
            // DONE? Should be fixed with exclusion of deleted newer files
            setForeground(NEWER_AVAILABLE);
            if (!newestVersion.isDeleted()) {
                setIcon(Icons.EXPECTED);
                statusForTooltip = Translation
                    .getTranslation("fileinfo.newversion_availeble");
            } else {
                if (newestVersion != newestDeletedVersion) {
                    if (folder != null
                        && SyncProfile.PROJECT_WORK.equals(folder.getSyncProfile()))
                    {
                        // Show remote deletions when in project work sync
                        setIcon(Icons.DELETE);
                        statusForTooltip = Translation
                            .getTranslation("fileinfo.remote_deleted");
                    }
                }
            }
        } else {
            setForeground(NORMAL);
        }

        // Okay basic infos added
        // Add meta info in tooltip now
        if (fInfo instanceof MP3FileInfo) {
            MP3FileInfo mp3FileInfo = (MP3FileInfo) fInfo;
            if (mp3FileInfo.isID3InfoValid()) {
                setToolTipText(getToolTipMp3(mp3FileInfo, fileNameForTooltip,
                    statusForTooltip));
            } else {
                setToolTipText(null);
            }
        } else if (fInfo instanceof ImageFileInfo) {
            ImageFileInfo imageFileInfo = (ImageFileInfo) fInfo;
            setToolTipText(getToolTipImg(imageFileInfo, fileNameForTooltip,
                statusForTooltip));
        } else {
            setToolTipText(getToolTip(fInfo, fileNameForTooltip,
                statusForTooltip));
        }
    }

    private String replaceSpacesWithNBSP(String text) {
        return text.replaceAll(" ", "&nbsp;");
    }

    private final String getToolTip(FileInfo fileInfo,
        String fileNameForTooltip, String statusForTooltip)
    {
        StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
        textInHTML
            .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
        textInHTML.append(".normal { font-size: 10px; color: #000000;}");
        textInHTML
            .append(".deleted { font-size: 10px; color: #FF0000;font-weight: bold;}");
        textInHTML
            .append(".new_avail { font-size: 10px; color: #0000FF;font-weight: bold;}");
        textInHTML
            .append(".bold { font-size: 10px; color: #000000;font-weight: bold;}");
        textInHTML.append("--></style>");
        textInHTML.append("</HEAD><BODY>");
        textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0>");
        if (!StringUtils.isBlank(fileNameForTooltip)) {
            textInHTML.append("<TR><TD valign=top class=bold colspan=2>&nbsp;"
                + fileNameForTooltip + "&nbsp;</TD></TR>");
        }

        if (!StringUtils.isBlank(statusForTooltip)) {
            String style;
            if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.deleted")))
            {
                style = "deleted";
            } else if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.newversion_availeble")))
            {
                style = "new_avail";
            } else {
                style = "bold";
            }
            textInHTML.append("<TR><TD valign=top class=" + style
                + " colspan=2>&nbsp;" + statusForTooltip + "&nbsp;</TD></TR>");
        }
        textInHTML.append("</TABLE>");
        // add hidden filename to make sure the tooltip is rendered on new spot
        // even if text is the same
        textInHTML.append("<!-- " + fileInfo.getFilenameOnly() + "-->");

        textInHTML.append("</BODY></HTML>");
        return textInHTML.toString();

    }

    private final String getToolTipImg(ImageFileInfo imageFileInfo,
        String fileNameForTooltip, String statusForTooltip)
    {
        StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
        textInHTML
            .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
        textInHTML.append(".normal { font-size: 10px; color: #000000;}");
        textInHTML
            .append(".deleted { font-size: 10px; color: #FF0000;font-weight: bold;}");
        textInHTML
            .append(".new_avail { font-size: 10px; color: #0000FF;font-weight: bold;}");
        textInHTML
            .append(".bold     { font-size: 10px; color: #000000;font-weight: bold;}");
        textInHTML.append("--></style>");
        textInHTML.append("</HEAD><BODY>");
        textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0>");
        if (!StringUtils.isBlank(fileNameForTooltip)) {
            textInHTML.append("<TR><TD valign=top class=bold colspan=2>&nbsp;"
                + fileNameForTooltip + "&nbsp;</TD></TR>");
        }
        if (!StringUtils.isBlank(statusForTooltip)) {
            String style;
            if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.deleted")))
            {
                style = "deleted";
            } else if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.newversion_availeble")))
            {
                style = "new_avail";
            } else {
                style = "bold";
            }
            textInHTML.append("<TR><TD valign=top class=" + style
                + " colspan=2>&nbsp;" + statusForTooltip + "&nbsp;</TD></TR>");
        }
        textInHTML.append("<TR><TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("imagefileinfo.resolution")
            + ":&nbsp;</TD><TD valign=top class=normal align=rigth>");
        if (imageFileInfo.getWidth() == -1 || imageFileInfo.getHeight() == -1) {
            textInHTML.append(Translation
                .getTranslation("imagefileinfo.unknown"));
        } else {
            textInHTML.append(imageFileInfo.getWidth() + "x"
                + imageFileInfo.getHeight());
        }
        textInHTML.append("&nbsp;</TD></TR>");
        textInHTML.append("</TABLE>");
        // add hidden filename to make sure the tooltip is rendered on new spot
        // even if text is the same
        textInHTML.append("<!-- " + imageFileInfo.getFilenameOnly() + "-->");

        textInHTML.append("</BODY></HTML>");
        return textInHTML.toString();
    }

    private final String getToolTipMp3(MP3FileInfo mp3FileInfo,
        String fileNameForTooltip, String statusForTooltip)
    {
        StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
        textInHTML
            .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
        textInHTML.append(".normal { font-size: 10px; color: #000000;}");
        textInHTML
            .append(".deleted { font-size: 10px; color: #FF0000;font-weight: bold;}");
        textInHTML
            .append(".new_avail { font-size: 10px; color: #0000FF;font-weight: bold;}");
        textInHTML
            .append(".bold { font-size: 10px; color: #000000;font-weight: bold;}");
        textInHTML
            .append(".red { font-size: 10px; color: #FF0000;font-weight: bold;}");
        textInHTML.append("--></style>");
        textInHTML.append("</HEAD><BODY>");
        textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0><TR>");
        if (!StringUtils.isBlank(fileNameForTooltip)) {
            textInHTML.append("<TD valign=top class=bold colspan=2>&nbsp;"
                + fileNameForTooltip + "&nbsp;</TD></TR>");
        }
        if (!StringUtils.isBlank(statusForTooltip)) {
            String style;
            if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.deleted")))
            {
                style = "deleted";
            } else if (statusForTooltip.equals(Translation
                .getTranslation("fileinfo.newversion_availeble")))
            {
                style = "new_avail";
            } else {
                style = "bold";
            }
            textInHTML.append("<TR><TD valign=top class=" + style
                + " colspan=2>&nbsp;" + statusForTooltip + "&nbsp;</TD></TR>");
        }
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.title")
            + ":</TD><TD valign=top class=normal align=rigth>"
            + replaceNullWithNA(mp3FileInfo.getTitle()) + "&nbsp;</TD></TR>");
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.artist")
            + ":</TD><TD valign=top class=normal align=rigth>"
            + replaceNullWithNA(mp3FileInfo.getArtist()) + "&nbsp;</TD></TR>");
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.album")
            + ":</TD><TD valign=top class=normal align=rigth>"
            + replaceNullWithNA(mp3FileInfo.getAlbum()) + "&nbsp;</TD></TR>");
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.size")
            + ":</TD><TD valign=top class=normal align=rigth>"
            + Format.formatBytes(mp3FileInfo.getSize())
            + " bytes&nbsp;</TD></TR>");
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.length")
            + ":</TD><TD valign=top class=normal align=rigth>"
            + mp3FileInfo.getLength() + " min:sec&nbsp;</TD></TR>");
        String style;
        if (mp3FileInfo.getBitrate() < 128) {
            style = "red";
        } else {
            style = "normal";
        }
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.bitrate")
            + ":</TD><TD valign=top  class=" + style + " align=rigth>"
            + mp3FileInfo.getBitrate() + " kbps&nbsp;</TD></TR>");

        if (mp3FileInfo.getSamplerate() < 40000) {
            style = "red";
        } else {
            style = "normal";
        }
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.samplerate")
            + ":</TD><TD valign=top  class=" + style + " align=rigth>"
            + Format.NUMBER_FORMATS.format(mp3FileInfo.getSamplerate())
            + "&nbsp;</TD></TR>");
        String text;
        if (mp3FileInfo.isStereo()) {
            style = "normal";
            text = "stereo";
        } else {
            style = "red";
            text = "mono";
        }
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
            + Translation.getTranslation("mp3fileinfo.stereo_mono")
            + ":&nbsp;</TD><TD valign=top  class=" + style + " align=rigth>"
            + text + "&nbsp;</TD></TR>");
        textInHTML.append("</TR></TABLE>");
        textInHTML.append("</BODY></HTML>");
        return textInHTML.toString();
    }

    private final static String replaceNullWithNA(String original) {
        return original == null ? "n/a" : original;
    }
}