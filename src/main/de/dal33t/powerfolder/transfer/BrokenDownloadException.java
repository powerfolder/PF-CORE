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
package de.dal33t.powerfolder.transfer;


public class BrokenDownloadException extends Exception {
    private static final long serialVersionUID = 1L;
    private final TransferProblem problem;

    public BrokenDownloadException(TransferProblem problem,
        Exception e)
    {
        super(e);
        this.problem = problem;
    }

    public BrokenDownloadException() {
        this(null, null);
    }

    public BrokenDownloadException(Throwable cause) {
        super(cause);
        problem = TransferProblem.BROKEN_DOWNLOAD;
    }

    public BrokenDownloadException(String message) {
        super(message);
        problem = TransferProblem.BROKEN_DOWNLOAD;
    }

}
