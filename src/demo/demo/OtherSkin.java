package demo;

import de.dal33t.powerfolder.skin.Skin;

public class OtherSkin implements Skin {

    public String getIconsPropertiesFileName() {
        return "OtherIcons.properties";
    }

    public Class getLookAndFeelClass() {
        return OtherLookAndFeel.class;
    }

    public String getName() {
        return "Other Skin";
    }
}