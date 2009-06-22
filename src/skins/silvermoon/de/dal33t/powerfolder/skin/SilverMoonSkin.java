package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel;

public class SilverMoonSkin implements Skin {

    private final String name;

    public SilverMoonSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaSilverMoonLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/SilverMoonIcons.properties";
    }
}