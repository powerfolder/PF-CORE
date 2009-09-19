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
package de.dal33t.powerfolder.ui;

import java.util.logging.Logger;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

/**
 * Class which offers several helper methods for handling with LookAndFeels.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class LookAndFeelSupport {

    private static final Logger log = Logger.getLogger(LookAndFeelSupport.class.getName());

    private LookAndFeelSupport() {
        // Only static methods available
    }

    /**
     * Sets the look and feel, and sets font so that Asian fonts display okay.
     * See http://www.javasoft.de/jsf/public/products/synthetica/faq#q12
     *
     * @param laf
     * @throws UnsupportedLookAndFeelException
     *
     */
    public static void setLookAndFeel(LookAndFeel laf)
            throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(laf);
        SyntheticaLookAndFeel.setFont("Dialog", 11);
    }
}