package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel;

public class BlueMoonSkin implements Skin {

    private final String name;

    public BlueMoonSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaBlueMoonLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlueMoonIcons.properties";
    }
}