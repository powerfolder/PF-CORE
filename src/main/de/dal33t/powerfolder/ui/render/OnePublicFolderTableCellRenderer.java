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
package de.dal33t.powerfolder.ui.render;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.ImageFileInfo;
import de.dal33t.powerfolder.light.MP3FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class OnePublicFolderTableCellRenderer extends DefaultTableCellRenderer {
    private Controller controller;

    /**
     * Initalizes a FileTableCellrenderer upon a <code>FileListTableModel</code>
     * 
     * @param controller
     *            the controller
     * @param tableModel
     *            the table model to act on
     */
    public OnePublicFolderTableCellRenderer(Controller controller) {
        this.controller = controller;

    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        setIcon(null);
        setToolTipText(null);
        Object fileOrDir = value;
        int columnInModel = UIUtil.toModel(table, column);
        if (fileOrDir instanceof FileInfo) {
            return render((FileInfo) fileOrDir, columnInModel, table,
                isSelected, hasFocus, row, column);
        }
        throw new IllegalStateException("expected FileInfo not: "
            + fileOrDir.getClass().getName());
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
                render0(fileInfo);
                newValue = fileInfo.getName();
                setToolTipText(newValue);
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
        }
        return super.getTableCellRendererComponent(table, newValue, isSelected,
            hasFocus, row, column);
    }

    private final void render0(FileInfo fInfo) {
        // Obtain the newest version of this file

        setForeground(Color.BLACK);

        // Okay basic infos added
        // Add meta info in tooltip now
        if (fInfo instanceof MP3FileInfo) {
            MP3FileInfo mp3FileInfo = (MP3FileInfo) fInfo;
            if (mp3FileInfo.isID3InfoValid()) {
                addJToolTip(mp3FileInfo);
            } else {
                setToolTipText(null);
            }
        } else if (fInfo instanceof ImageFileInfo) {
            ImageFileInfo imageFileInfo = (ImageFileInfo) fInfo;
            addJToolTip(imageFileInfo);
        } else {
            setToolTipText(null);
        }
    }

    private final void addJToolTip(ImageFileInfo imageFileInfo) {
        StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
        textInHTML
            .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
        // .append("<style TYPE=\"text/css\"><!--BODY { font-family:
        // Verdana,Arial, Helvetica, sans-serif; font-size: 10px; color:
        // #000000; background : #FFFFFF; }");
        textInHTML.append(".normal     { font-size: 10px; color: #000000;}");
        textInHTML
            .append(".bold     { font-size: 10px; color: #000000;font-weight: bold;}");
        textInHTML.append("--></style>");
        textInHTML.append("</HEAD><BODY>");
        textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0><TR>");
        textInHTML.append("<TD valign=top class=bold>&nbsp;"
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
        setToolTipText(textInHTML.toString());
    }

    private final void addJToolTip(MP3FileInfo mp3FileInfo) {
        StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
        textInHTML
            .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
        textInHTML.append(".normal     { font-size: 10px; color: #000000;}");
        textInHTML
            .append(".bold     { font-size: 10px; color: #000000;font-weight: bold;}");
        textInHTML
            .append(".red     { font-size: 10px; color: #FF0000;font-weight: bold;}");
        textInHTML.append("--></style>");
        textInHTML.append("</HEAD><BODY>");
        textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0><TR>");
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
            + Format.getNumberFormat().format(mp3FileInfo.getSamplerate())
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
        setToolTipText(textInHTML.toString());
    }

    private final static String replaceNullWithNA(String original) {
        return original == null ? "n/a" : original;
    }
}
