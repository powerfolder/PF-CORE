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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collection;

/**
 * Renderer for any transfer table
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.20 $
 */
public class DownloadManagerTableCellRenderer extends DefaultTableCellRenderer {
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
    public DownloadManagerTableCellRenderer(Controller controller) {
        this.controller = controller;

        // FIXME: Find a better way to set text color of progress bar string
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK);
        UIManager.put("ProgressBar.selectionForeground", Color.BLACK);
        // UIManager.put("ProgressBar.foreground", Color.WHITE);

        bar = new JProgressBar();
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
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Component defaultComp = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

        if (value instanceof DownloadManager) {
            DownloadManager downloadManager = (DownloadManager) value;
            TransferCounter counter = downloadManager.getCounter();

            // Show bar
            bar.setValue((int) (Math.max(0, downloadManager.getState().getProgress()) * 100));
            bar.setBackground(defaultComp.getBackground());

            Transfer.State state = downloadManager.getState();
            if (state == null) {
                state = new Transfer.State();
                state.setState(Transfer.TransferState.NONE);
            }

            switch (state.getState()) {
                case MATCHING:
                case VERIFYING:
                case FILERECORD_REQUEST:
                case COPYING:
                    bar.setString(Translation.getTranslation(state.getState()
                            .getTranslationId()));
                    break;
                case DOWNLOADING:
                    EstimatedTime et = new EstimatedTime(downloadManager
                            .getCounter().calculateEstimatedMillisToCompletion(),
                            downloadManager.isCompleted() && downloadManager.isStarted());
                    String kbs = Translation.getTranslation(
                            "transfers.kbs", Format.getNumberFormat()
                            .format(counter.calculateCurrentKBS()));
                    String text = (et.isActive() ? et.toString()
                            + " - " : "")
                            + kbs;
                    bar.setString(text);
                    break;

                default:
                    if (downloadManager.isCompleted()) {
                        bar.setString(Translation
                                .getTranslation("transfers.completed"));
                    } else if (downloadManager.isStarted()) {
                        bar.setString(Translation
                                .getTranslation("transfers.started"));
                    } else {
                        bar.setString(Translation
                                .getTranslation("transfers.requested"));
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
        } else if (value instanceof Collection) {
            java.util.Collection<Download> sources = (java.util.Collection<Download>) value;
            if (sources.isEmpty()) { // This happens on abort
                setText("");
                setIcon(null);
            } else {
                Download primaryDownload = sources.iterator().next();
                if (sources.size() == 1) {
                    String nickText = primaryDownload.getPartner().getNick();
                    setText(nickText);
                } else {
                    setText(Translation.getTranslation("transfers.swarm", sources.size()));
                }
                setIcon(Icons.getSimpleIconFor(primaryDownload.getPartner()));
                setHorizontalAlignment(LEFT);
            }
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