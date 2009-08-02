package de.dal33t.powerfolder.skin;

import java.text.ParseException;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

public class PowerFolder1Skin implements Skin {

    public String getName() {
        return "PowerFolder Skin 1";
    }

    public Class<? extends LookAndFeel> getLookAndFeelClass() {
        return LookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/PowerFolder1Icons.properties";
    }

    public static class LookAndFeel extends SyntheticaLookAndFeel {
        private static final long serialVersionUID = 1L;

        public LookAndFeel() throws ParseException {
            super("/de/dal33t/powerfolder/skin/powerfolder1/synth.xml");
        }

        @Override
        public String getID() {
            return "PowerFolderSkin1";
        }

        @Override
        public String getName() {
            return "PowerFolder Skin 1";
        }
    }
}
