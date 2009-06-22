package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel;

public class BlueSteelSkin implements Skin {

    private final String name;

    public BlueSteelSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaBlueSteelLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlueSteelIcons.properties";
    }
}