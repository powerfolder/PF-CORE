package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel;

public class OrangeMetallicSkin implements Skin {

    private final String name;

    public OrangeMetallicSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaOrangeMetallicLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/OrangeMetallicIcons.properties";
    }
}