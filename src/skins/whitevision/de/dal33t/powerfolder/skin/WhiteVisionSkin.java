package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel;

public class WhiteVisionSkin implements Skin {

    private final String name;

    public WhiteVisionSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaWhiteVisionLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/WhiteVisionIcons.properties";
    }
}