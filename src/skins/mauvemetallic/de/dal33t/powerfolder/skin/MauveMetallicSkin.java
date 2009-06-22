package de.dal33t.powerfolder.skin;

import de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel;

public class MauveMetallicSkin implements Skin {

    private final String name;

    public MauveMetallicSkin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getLookAndFeelClass() {
        return SyntheticaMauveMetallicLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/MauveMetallicIcons.properties";
    }
}