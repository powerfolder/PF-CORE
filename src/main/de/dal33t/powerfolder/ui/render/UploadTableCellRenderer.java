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

import java.awt.Component;
import java.util.Date;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.EstimatedTime;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;

/**
 * Renderer for any transfer table
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.20 $
 */
public class UploadTableCellRenderer extends DefaultTableCellRenderer {
    // private static final Logger LOG =
    // Logger.getLogger(TransferTableCellRenderer.class);
    private Controller controller;
    private JProgressBar bar;

    /**
     * Initalizes the renderer for a transfertable. renderDownloads determines
     * if we are rendering ul or dls. Maybe split this class up into two
     *
     * @param controller
     */
    public UploadTableCellRenderer(Controller controller) {
        this.controller = controller;

        this.bar = new JProgressBar();
        bar.setBorderPainted(false);
        bar.setBorder(Borders.EMPTY_BORDER);
        bar.setStringPainted(true);

        // Listen for ui l&f changes
        UIUtil.addUIChangeTask(new Runnable() {
            public void run() {
                bar.updateUI();
            }
        });
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        Component defaultComp = super.getTableCellRendererComponent(table,
            value, isSelected, hasFocus, row, column);

        if (value instanceof Transfer) {
            Transfer transfer = (Transfer) value;
            TransferCounter counter = transfer.getCounter();

            // Show bar
            bar.setValue((int) (Math.max(0, transfer.getStateProgress()) * 100));
            // bar.setBackground(defaultComp.getBackground());

            if (value instanceof Download) {
                Download download = (Download) transfer;
                if (download.getTransferProblem() != null
                    && shouldShowProblem(download.getTransferProblem()))
                {
                    TransferProblem transferProblem = download
                        .getTransferProblem();
                    String problemInformation = download
                        .getProblemInformation();
                    if (problemInformation == null) {
                        bar.setString(Translation
                            .getTranslation(transferProblem.getTranslationId()));
                    } else {
                        bar.setString(Translation.getTranslation(
                            transferProblem.getTranslationId(),
                            problemInformation));
                    }
                } else {
                    Transfer.TransferState state = download.getTransferState();
                    if (state == null) {
                        state = Transfer.TransferState.NONE;
                    }

                    switch (state) {
                        case MATCHING :
                        case VERIFYING :
                        case FILERECORD_REQUEST :
                        case COPYING :
                            bar.setString(Translation.getTranslation(state
                                .getTranslationId()));
                            break;
                        case DOWNLOADING :
                            EstimatedTime et = new EstimatedTime(download
                                .getDownloadManager().getCounter()
                                .calculateEstimatedMillisToCompletion(),
                                !download.isCompleted() && download.isStarted());
                            // EstimatedTime et = new EstimatedTime(download
                            // .getCounter()
                            // .calculateEstimatedMillisToCompletion(),
                            // !download.isCompleted() && download.isStarted());
                            String kbs = Translation.getTranslation(
                                "transfers.kbs", Format.formatDecimal(counter
                                    .calculateCurrentKBS()));
                            String text = (et.isActive() ? et.toString()
                                + " - " : "")
                                + kbs;
                            bar.setString(text);
                            break;

                        default :
                            if (download.isCompleted()) {
                                Date completedDate = download
                                    .getCompletedDate();
                                String dateStr = completedDate != null ? Format
                                    .formatDateShort(completedDate, true) : "";
                                bar.setString(Translation.getTranslation(
                                    "transfers.completed", dateStr));
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
            } else if (value instanceof Upload) {
                Upload upload = (Upload) transfer;
                switch (upload.getTransferState()) {
                    case FILEHASHING :
                    case REMOTEMATCHING :
                        bar.setString(Translation.getTranslation(upload
                            .getTransferState().getTranslationId()));
                        break;
                    default :
                        if (upload.isCompleted()) {
                            Date completedDate = transfer.getCompletedDate();
                            String dateStr = completedDate != null ? Format
                                .formatDateShort(completedDate, true) : "";
                            bar.setString(Translation.getTranslation(
                                "transfers.completed", dateStr));
                        } else if (upload.isStarted()) {
                            bar.setString(Translation.getTranslation(
                                "transfers.kbs", Format.formatDecimal(counter
                                    .calculateCurrentKBS())));
                        } else {
                            bar.setString(Translation
                                .getTranslation("transfers.queued"));
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
                setIcon(null);
                setHorizontalAlignment(LEFT);
            }
        } else if (value instanceof Long) {
            Long size = (Long) value;
            setText(Format.formatBytesShort(size));
            setIcon(null);
            setHorizontalAlignment(RIGHT);
        } else if (value instanceof FolderInfo) {
            FolderInfo foInfo = (FolderInfo) value;
            setText(foInfo.getLocalizedName());
            setHorizontalAlignment(LEFT);
        } else if (value instanceof Member) {
            Member node = (Member) value;
            String nickText = node.getNick();
            if (node.isOnLAN()) {
                nickText += " ("
                    + Translation.getTranslation("transfers.local") + ')';
            }
            setText(nickText);
            setHorizontalAlignment(LEFT);
        } else if (value instanceof MemberInfo) {
            MemberInfo node = (MemberInfo) value;
            String nickText = node.nick;
            setText(nickText);
            setIcon(null);
            setHorizontalAlignment(LEFT);
        } else if (value instanceof EstimatedTime) {
            EstimatedTime time = (EstimatedTime) value;
            if (time.isActive()) {
                setText(Format.formatDeltaTime(time.getDeltaTimeMillis()));
            } else {
                setText("");
            }

            setIcon(null);
            setHorizontalAlignment(CENTER);
        } else {
            setText(Translation.getTranslation("transfers.searching"));
            setIcon(null);
            setHorizontalAlignment(LEFT);
        }

        if (!isSelected) {
            setBackground(row % 2 == 0
                ? ColorUtil.EVEN_TABLE_ROW_COLOR
                : ColorUtil.ODD_TABLE_ROW_COLOR);
        }

        return defaultComp;
    }

    /**
     * Only some types of problem are relevant for display.
     * <p>
     * TODO COPIED from DownloadsTableModel
     *
     * @param problem
     *            the transfer problem
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