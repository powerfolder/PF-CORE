package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel;

public class GreenDreamSkin implements Skin {

    private final String name;

    public GreenDreamSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaGreenDreamLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/GreenDreamIcons.properties";
    }
}
