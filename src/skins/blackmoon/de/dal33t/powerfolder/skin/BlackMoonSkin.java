package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel;

public class BlackMoonSkin implements Skin {

    private final String name;

    public BlackMoonSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaBlackMoonLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlackMoonIcons.properties";
    }
}