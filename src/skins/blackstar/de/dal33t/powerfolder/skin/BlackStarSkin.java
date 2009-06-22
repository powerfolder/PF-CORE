package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel;

public class BlackStarSkin implements Skin {

    private final String name;

    public BlackStarSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaBlackStarLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlackStarIcons.properties";
    }
}
