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
import java.io.FileNotFoundException;
import java.io.IOException;
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
        File file = new File(filename);
        if (!file.exists()) {
            throw new FileNotFoundException(filename);
        }
        FileInputStream in = new FileInputStream(file);
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
