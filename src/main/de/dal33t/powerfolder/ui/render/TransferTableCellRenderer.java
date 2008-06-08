/* $Id: TransferTableCellRenderer.java,v 1.20 2006/04/15 19:19:04 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.render;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Renderer for any transfer table
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.20 $
 */
public class TransferTableCellRenderer extends DefaultTableCellRenderer {
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
    public TransferTableCellRenderer(Controller controller) {
        this.controller = controller;

        // FIXME: Find a better way to set text color of progress bar string
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK);
        UIManager.put("ProgressBar.selectionForeground", Color.BLACK);
        // UIManager.put("ProgressBar.foreground", Color.WHITE);

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
            bar
                .setValue((int) (Math.max(0, transfer.getStateProgress()) * 100));
            bar.setBackground(defaultComp.getBackground());

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
                        bar
                            .setString(Translation
                                .getTranslation(transferProblem
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
//                            EstimatedTime et = new EstimatedTime(download
//                                .getCounter()
//                                .calculateEstimatedMillisToCompletion(),
//                                !download.isCompleted() && download.isStarted());
                            String kbs = Translation.getTranslation(
                                "transfers.kbs", Format.getNumberFormat()
                                    .format(counter.calculateCurrentKBS()));
                            String text = (et.isActive() ? et.toString()
                                + " - " : "")
                                + kbs;
                            bar.setString(text);
                            break;

                        default :
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
            } else if (value instanceof Upload) {
                Upload upload = (Upload) transfer;
                switch (upload.getState()) {
                    case FILEHASHING :
                    case REMOTEMATCHING :
                        bar.setString(Translation.getTranslation(upload
                            .getState().getTranslationId()));
                        break;
                    default :
                        if (upload.isCompleted()) {
                            bar.setString(Translation
                                .getTranslation("transfers.completed"));
                        } else if (upload.isStarted()) {
                            bar.setString(Translation.getTranslation(
                                "transfers.kbs", Format.getNumberFormat()
                                    .format(counter.calculateCurrentKBS())));
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
                setHorizontalAlignment(SwingConstants.LEFT);
            }
        } else if (value instanceof Long) {
            Long size = (Long) value;
            setText(Format.formatBytesShort(size));
            setIcon(null);
            setHorizontalAlignment(SwingConstants.RIGHT);
        } else if (value instanceof FolderInfo) {
            FolderInfo foInfo = (FolderInfo) value;
            setText(foInfo.name);
            setIcon(Icons.FOLDER);
            setHorizontalAlignment(SwingConstants.LEFT);
        } else if (value instanceof Member) {
            Member node = (Member) value;
            String nickText = node.getNick();
            if (node.isOnLAN()) {
                nickText += " ("
                    + Translation.getTranslation("transfers.local") + ')';
            }
            setText(nickText);
            setIcon(Icons.getSimpleIconFor(node));
            setHorizontalAlignment(SwingConstants.LEFT);
        } else if (value instanceof MemberInfo) {
            MemberInfo node = (MemberInfo) value;
            String nickText = node.nick;

            setText(nickText);
            setIcon(null);
            setHorizontalAlignment(SwingConstants.LEFT);
        } else if (value instanceof EstimatedTime) {
            EstimatedTime time = (EstimatedTime) value;
            if (time.isActive()) {
                setText(Format.formatDeltaTime(time.getDeltaTimeMillis()));
            } else {
                setText("");
            }

            setIcon(null);
            setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            setText(Translation.getTranslation("transfers.searching"));
            setIcon(null);
            setHorizontalAlignment(SwingConstants.LEFT);
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