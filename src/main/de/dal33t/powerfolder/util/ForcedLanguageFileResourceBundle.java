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
* $Id: ForcedLanguageFileResourceBundle.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/*******************************************************************************
 * Class that loads a ResourceBundle from a file, used to force the translation
 * file in the commandline.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 ******************************************************************************/
public class ForcedLanguageFileResourceBundle extends ResourceBundle {
    Properties props = new Properties();

    public ForcedLanguageFileResourceBundle(String filename)
        throws FileNotFoundException, IOException
    {
        super();
        Path file = Paths.get(filename);
        if (Files.notExists(file)) {
            throw new FileNotFoundException(filename);
        }
        InputStream in = Files.newInputStream(file);
        props.load(in);
        in.close();
    }

    protected Object handleGetObject(String key) {
        return props.get(key);
    }

    public Enumeration getKeys() {
        return props.keys();
    }
}
