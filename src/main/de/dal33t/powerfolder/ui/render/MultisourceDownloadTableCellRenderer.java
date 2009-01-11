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

import com.jgoodies.forms.factories.Borders;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.downloads.MultisourceDownload;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EstimatedTime;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.Iterator;

/**
 * Renderer for multisource downloads table
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.20 $
 */
public class MultisourceDownloadTableCellRenderer extends DefaultTableCellRenderer {

    private Controller controller;
    private JProgressBar bar;

    /**
     * Initalizes the renderer for a MSDownload.
     *
     * @param controller
     */
    public MultisourceDownloadTableCellRenderer(Controller controller) {
        this.controller = controller;

        // FIXME: Find a better way to set text color of progress bar string
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK);
        UIManager.put("ProgressBar.selectionForeground", Color.BLACK);

        bar = new JProgressBar();
        bar.setBorderPainted(false);
        bar.setBorder(Borders.EMPTY_BORDER);
        bar.setStringPainted(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Component defaultComp = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
        setToolTipText(null);

        if (value instanceof MultisourceDownload) {
            MultisourceDownload download = (MultisourceDownload) value;

            // Show bar
            bar.setValue(Math.max(0, (int) download.getStateProgress()) * 100);
            bar.setBackground(defaultComp.getBackground());

            if (download.getTransferProblem() != null
                    && shouldShowProblem(download.getTransferProblem())) {
                TransferProblem transferProblem = download
                        .getTransferProblem();
                String problemInformation = download
                        .getProblemInformation();
                if (problemInformation == null) {
                    bar.setString(Translation.getTranslation(transferProblem
                            .getTranslationId()));
                } else {
                    bar.setString(Translation.getTranslation(
                            transferProblem.getTranslationId(),
                            problemInformation));
                }
            } else {
                Transfer.TransferState state = download.getState();
                if (state == null) {
                    state = Transfer.TransferState.NONE;
                }

                switch (state) {
                    case MATCHING:
                    case VERIFYING:
                    case FILERECORD_REQUEST:
                    case COPYING:
                        bar.setString(Translation.getTranslation(state
                                .getTranslationId()));
                        break;
                    case DOWNLOADING:
                        EstimatedTime et = new EstimatedTime(
                                download.calculateEstimatedMillisToCompletion(),
                                !download.isCompleted() && download.isStarted());
                        String kbs = Translation.getTranslation(
                                "transfers.kbs", Format.getNumberFormat()
                                .format(download.calculateCurrentKBS()));
                        String text = (et.isActive() ? et.toString()
                                + " - " : "")
                                + kbs;
                        bar.setString(text);
                        break;

                    default:
                        if (download.isCompleted()) {
                            bar.setString(Translation
                                    .getTranslation("transfers.completed"));
                        } else if (download.isQueued()) {
                            bar.setString(Translation
                                    .getTranslation("transfers.queued"));
                        } else if (download.isPending()) {
                            bar.setString(Translation
                                    .getTranslation("transfers.pending"));
                        } else {
                            bar.setString(Translation
                                    .getTranslation("transfers.requested"));
                        }
                }
            }
            return bar;
        } else if (value instanceof FileInfo) {
            if (column == 0) { // File type
                FileInfo fInfo = (FileInfo) value;
                setIcon(Icons.getEnabledIconFor(fInfo, controller));
                setText("");
            } else { // File info
                FileInfo fInfo = (FileInfo) value;
                setText(fInfo.getFilenameOnly());
                Folder folder = fInfo.getFolder(controller
                        .getFolderRepository());
                if (folder != null && folder.getDiskItemFilter().isExcluded(fInfo)) {
                    if (folder.isWhitelist()) {
                        setIcon(Icons.WHITE_LIST);
                    } else {
                        setIcon(Icons.BLACK_LIST);
                    }
                } else {
                    setIcon(null);
                }
                setHorizontalAlignment(LEFT);
            }
        } else if (value instanceof Long) {
            Long size = (Long) value;
            setText(Format.formatBytesShort(size));
            setIcon(null);
            setHorizontalAlignment(RIGHT);
        } else if (value instanceof FolderInfo) {
            FolderInfo foInfo = (FolderInfo) value;
            setText(foInfo.name);
            setIcon(Icons.FOLDER);
            setHorizontalAlignment(LEFT);
        } else if (value instanceof List) {
            List<Member> list = (List<Member>) value;
            Member primaryNode = null;
            if (list.size() == 1) {
                primaryNode = list.get(0);
                StringBuilder sb = new StringBuilder();
                sb.append(primaryNode.getNick());
                if (primaryNode.isOnLAN()) {
                    sb.append(" (" + Translation.getTranslation(
                            "transfers.local") + ')');
                }
                setText(sb.toString());
            } else {
                StringBuilder sb = new StringBuilder();
                for (Iterator<Member> iter = list.iterator(); iter.hasNext();) {
                    Member node = iter.next();
                    if (primaryNode == null) {
                        primaryNode = node;
                    }
                    sb.append(node.getNick());
                    if (node.isOnLAN()) {
                        sb.append(" (" + Translation.getTranslation(
                                "transfers.local") + ')');
                    }
                    if (iter.hasNext()) {
                        sb.append(", ");
                    }
                }
                setToolTipText(sb.toString());
                setText(Translation.getTranslation("transfers.swarm", list.size()));
            }
            setIcon(Icons.getSimpleIconFor(primaryNode));
            setHorizontalAlignment(LEFT);
        } else {
            setText(Translation.getTranslation("transfers.searching"));
            setIcon(null);
            setHorizontalAlignment(LEFT);
        }

        return defaultComp;
    }

    /**
     * Only some types of problem are relevant for display.
     * <p/>
     * TODO COPIED from DownloadsTableModel
     *
     * @param problem the transfer problem
     * @return true if it should be displayed.
     */
    private static boolean shouldShowProblem(TransferProblem problem) {
        return TransferProblem.FILE_NOT_FOUND_EXCEPTION.equals(problem)
                || TransferProblem.IO_EXCEPTION.equals(problem)
                || TransferProblem.TEMP_FILE_DELETE.equals(problem)
                || TransferProblem.TEMP_FILE_OPEN.equals(problem)
                || TransferProblem.TEMP_FILE_WRITE.equals(problem)
                || TransferProblem.MD5_ERROR.equals(problem);
    }
}