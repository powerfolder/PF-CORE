/*
s * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: WinUtils.java 14924 2011-03-10 20:09:51Z tot $
 */
package de.dal33t.powerfolder.util.os.mac;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Utilities for Mac OS X
 * 
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 */
public class MacUtils extends Loggable {
    private static final Logger LOG = Logger
        .getLogger(MacUtils.class.getName());

    private static MacUtils instance;
    private static boolean error = false;
    private static String placesHelperPath;

    private MacUtils() {
    }

    /**
     * @return true if this platform supports the winutils helpers.
     */
    public static boolean isSupported() {
        return getInstance() != null && !error;
    }

    /**
     * @return the instance or NULL if not supported on this platform.
     */
    public static synchronized MacUtils getInstance() {
        if (!OSUtil.isMacOS()) {
            return null;
        }

        if (instance == null && !error) {
            if (OSUtil.isMacOS()) {
                instance = new MacUtils();
                error = !instance.init();
            } else {
                error = true;
            }
        }
        return instance;
    }

    public void createPlacesLink(String lnkTarget) throws IOException {
       String cmdLine = placesHelperPath + " p " + lnkTarget;
       Runtime.getRuntime().exec(cmdLine);
    }

    private boolean init() {
        String fileName = "placeshelper";
        File targetFile = new File(Controller.getTempFilesLocation(), fileName);
        targetFile.deleteOnExit();
        File file = Util.copyResourceTo("mac/" + fileName, null, targetFile,
            false);
        placesHelperPath = file.getAbsolutePath();
        try {
            Runtime.getRuntime().exec("chmod +x " + placesHelperPath);
        } catch (IOException e) {
            LOG.warning("Unable to initialize mac helper files. " + e);
            return false;
        }
        return file.exists();

    }

    /**
     * Create a 'PowerFolders' link in Links, pointing to the PowerFolder base
     * dir.
     * 
     * @param setup
     * @param controller
     * @throws IOException
     */
    public void setPFPlaces(boolean setup, Controller controller)
        throws IOException
    {
      if (setup) {
          File baseDir = controller.getFolderRepository().getFoldersAbsoluteDir();
          createPlacesLink(baseDir.getAbsolutePath());
      } else {
          // TODO Remove places link
      }
    }

    public void setPFStartup(boolean setup, Controller controller)
        throws IOException
    {

    }

}
