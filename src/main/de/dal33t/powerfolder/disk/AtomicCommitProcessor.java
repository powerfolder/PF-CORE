/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: TransferManager.java 13068 2010-07-21 17:07:56Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Visitor;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * #2056: Handles the directory to commit/mirror the whole folder to when it
 * reaches 100% sync (no incoming files and at least 1 other member).
 * 
 * @author sprajc
 */
public class AtomicCommitProcessor extends Loggable {
    public static AtomicCommitProcessor INSTANCE = new AtomicCommitProcessor();

    /**
     * Checks and performs the atomic commit / mirror to the commit dir of the
     * folder if set.
     * 
     * @param folder
     * @return true if the miorror/copy has been performed. false if not.
     */
    public boolean perform(final Folder folder) {
        Reject.ifNull(folder, "Folder");
        if (folder.getCommitDir() == null) {
            if (isFiner()) {
                logFiner("Not performing atomic commit. No commit dir");
            }
            return false;
        }
        if (folder.getConnectedMembersCount() < 1) {
            logWarning("Not performing atomic commit to "
                + folder.getCommitDir() + ". No other member connected");
            return false;
        }
        IncomingCheckVisitor vistor = new IncomingCheckVisitor();
        folder.visitIncomingFiles(vistor);
        if (vistor.hasIncoming) {
            if (isFiner()) {
                logFiner("Not performing atomic commit/mirror to "
                    + folder.getCommitDir()
                    + ". Not in sync yet / has incoming files.");
            }
            return false;
        }
        try {
            FileUtils.recursiveMirror(folder.getLocalBase(), folder
                .getCommitDir(), new FileFilter() {
                public boolean accept(File pathname) {
                    return !pathname.equals(folder.getLocalBase())
                        && !pathname.equals(folder.getSystemSubDir());
                }
            });
            logWarning("Mirrored from " + folder.getLocalBase() + " to "
                + folder.getCommitDir());
            return true;
        } catch (IOException e) {
            logWarning("Problem while committing to " + folder.getCommitDir()
                + ". " + e, e);
            return false;
        }
    }

    private static class IncomingCheckVisitor implements Visitor<FileInfo> {
        private boolean hasIncoming;

        public boolean visit(FileInfo fileInfo) {
            hasIncoming = true;
            return false;
        }
    }
}
