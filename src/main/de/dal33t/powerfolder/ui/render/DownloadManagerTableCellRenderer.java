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
import java.util.Collection;
import java.util.Date;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.Transfer;
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
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        Component defaultComp = super.getTableCellRendererComponent(table,
            value, isSelected, hasFocus, row, column);

        if (value instanceof DownloadManager) {
            DownloadManager downloadManager = (DownloadManager) value;
            TransferCounter counter = downloadManager.getCounter();

            // Show bar
            bar.setValue((int) (Math.max(0, downloadManager.getState()
                .getProgress()) * 100));
            // bar.setBackground(defaultComp.getBackground());

            Transfer.State state = downloadManager.getState();
            if (state == null) {
                state = new Transfer.State();
                state.setState(Transfer.TransferState.NONE);
            }

            switch (state.getState()) {
                case VERIFYING :
                    bar.setString(Translation.getTranslation(state.getState()
                        .getTranslationId()));
                    bar.setValue(100);
                    break;
                case MATCHING :
                case FILERECORD_REQUEST :
                case COPYING :
                    bar.setString(Translation.getTranslation(state.getState()
                        .getTranslationId()));
                    break;
                case DOWNLOADING :
                    EstimatedTime et = new EstimatedTime(downloadManager
                        .getCounter().calculateEstimatedMillisToCompletion(),
                        downloadManager.isCompleted()
                            && downloadManager.isStarted());
                    String kbs = Translation.getTranslation("transfers.kbs",
                        Format.formatDecimal(counter.calculateCurrentKBS()));
                    String text = (et.isActive() ? et.toString() + " - " : "")
                        + kbs;
                    bar.setString(text);
                    break;

                default :
                    if (downloadManager.isCompleted()) {
                        Date completedDate = downloadManager.getCompletedDate();
                        String dateStr = completedDate != null ? Format
                            .formatDateShort(completedDate, true) : "";
                        bar.setValue(100);
                        bar.setString(Translation.getTranslation(
                            "transfers.completed", dateStr));
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
        } else if (value instanceof Collection) {
            Collection<Download> sources = (Collection<Download>) value;
            if (sources.isEmpty()) { // This happens on abort
                setText("");
                setIcon(null);
            } else {
                Download primaryDownload = sources.iterator().next();
                if (sources.size() == 1) {
                    String nickText = primaryDownload.getPartner().getNick();
                    setText(nickText);
                } else {
                    setText(Translation.getTranslation("transfers.swarm",
                        String.valueOf(sources.size())));
                }
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

        if (!isSelected) {
            setBackground(row % 2 == 0
                ? ColorUtil.EVEN_TABLE_ROW_COLOR
                : ColorUtil.ODD_TABLE_ROW_COLOR);
        }

        return defaultComp;
    }
}