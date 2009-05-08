package demo;

import de.dal33t.powerfolder.skin.Skin;

public class CustomSkin implements Skin {

    public String getIconsPropertiesFileName() {
        return "CustomIcons.properties";
    }

    public Class getLookAndFeelClass() {
        return CustomLookAndFeel.class;
    }

    public String getName() {
        return "Custom Skin";
    }
}
