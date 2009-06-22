package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel;

public class SkyMetallicSkin implements Skin {

    private final String name;

    public SkyMetallicSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaSkyMetallicLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/SkyMetallicIcons.properties";
    }
}