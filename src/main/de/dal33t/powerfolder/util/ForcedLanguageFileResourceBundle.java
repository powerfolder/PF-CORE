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
            throw new FileNotFoundException();
        }
        props.load(new FileInputStream(file));
    }

    protected Object handleGetObject(String key) {
        return props.get(key);
    }

    public Enumeration getKeys() {
        return props.keys();
    }
}
