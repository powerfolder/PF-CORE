package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.util.Translation;

/**
 * Contains useful and only available presets for the filefilter.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum FileFilterPreset {
    ALL("all", true, true, true, true),
    LOCAL("local", true, false, false, false),
    INCOMING("incoming", false, true, false, false),
    DELETED("deleted", false, false, true, false),
    INGNORED("ignored", false, false, false, true);

    // Methods/Constructors ***************************************************

    private static final String TRANSLATION_ID_PREFIX = "filefilter.preset.";
    private String translationId;
    private boolean showLocal;
    private boolean showExpected;
    private boolean showDeleted;
    private boolean showIgnored;

    private FileFilterPreset(String translationIdSuffix, boolean showNormal,
        boolean showExpected, boolean showDeleted, boolean showIgnored)
    {
        this.translationId = TRANSLATION_ID_PREFIX + translationIdSuffix;
        this.showLocal = showNormal;
        this.showExpected = showExpected;
        this.showDeleted = showDeleted;
        this.showIgnored = showIgnored;
    }

    /**
     * @return the translated name of the preset
     */
    public String getTranslatedName() {
        return Translation.getTranslation(translationId);
    }

    public boolean isShowDeleted() {
        return showDeleted;
    }

    public boolean isShowExpected() {
        return showExpected;
    }

    public boolean isShowIgnored() {
        return showIgnored;
    }

    public boolean isShowNormal() {
        return showLocal;
    }
}
