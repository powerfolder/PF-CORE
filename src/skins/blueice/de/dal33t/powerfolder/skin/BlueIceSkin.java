package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel;

public class BlueIceSkin implements Skin {

    private final String name;

    public BlueIceSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaBlueIceLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlueIceIcons.properties";
    }
}