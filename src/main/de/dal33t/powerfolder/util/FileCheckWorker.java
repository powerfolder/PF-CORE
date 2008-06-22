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
package de.dal33t.powerfolder.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class FileCheckWorker implements Callable<Boolean>{

    private final File fileToCheck;
    private final MessageDigest digest;
    private final byte[] expectedHash;

    public FileCheckWorker(File fileToCheck, MessageDigest digest, byte[] expectedHash) {
        Reject.noNullElements(fileToCheck, digest, expectedHash);
        this.fileToCheck = fileToCheck;
        this.digest = digest;
        this.expectedHash = expectedHash;
        
    }
    
    public Boolean call() throws Exception {
        FileInputStream in = new FileInputStream(fileToCheck);
        try {
            byte[] data = new byte[8192];
            long len = fileToCheck.length();
            long rem = len;
            int read;
            while ((read = in.read(data)) > 0) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                digest.update(data, 0, read);
                rem -= read;
                setProgress((int) (100.0 - rem * 100.0 / len));
            }
            byte digestResult[] = digest.digest();
            return Arrays.equals(digestResult, expectedHash); 
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected void setProgress(int percent) {
    }

}
