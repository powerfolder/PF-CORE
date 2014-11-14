package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.Translation;
import junit.framework.TestCase;

public class TranslationTest extends TestCase {

    public void testParams() {
        String text = Translation
            .getTranslation("uicontroller.remote_mass_delete.warning_message");
        assertTrue("Param key missing: " + text, text.contains("{0}"));
        assertTrue("Param key missing: " + text, text.contains("{1}"));
        assertTrue("Param key missing: " + text, text.contains("{2}"));
        assertTrue("Param key missing: " + text, text.contains("{3}"));
        assertTrue("Param key missing: " + text, text.contains("{4}"));
        text = Translation.getTranslation(
            "uicontroller.remote_mass_delete.warning_message", "PARAM0",
            "PARAM1", "PARAM2", "PARAM3", "PARAM4");
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM0"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM1"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM2"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM3"));
        assertTrue("Param replacement missing: " + text, text
            .contains("PARAM4"));
    }

    public void testPlaceholders() {
        String text = Translation
            .getTranslation("action_login.description");
        assertTrue("Text fail: " + text, text.contains("PowerFolder"));
        assertFalse("Text fail: " + text, text.contains("{PowerFolder}"));
        Translation.setPlaceHolder("APPNAME", "XXX");
        text = Translation.getTranslation("action_login.description");
        assertTrue("Text fail: " + text, text.contains("XXX"));
        assertFalse("Text fail: " + text, text.contains("{XXX}"));

        Translation.setPlaceHolder("APPNAME", null);
        text = Translation.getTranslation("action_login.description");
        assertTrue("Text fail: " + text, text.contains("{APPNAME}"));
    }
}
