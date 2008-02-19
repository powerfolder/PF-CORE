package de.dal33t.powerfolder.util.ui;

/**
 * Utility enum that is used to define icons in {@link DialogFactory}.
 */
public enum GenericDialogType {

    /** the default icon reference */
    DEFAULT("default"),
    /** the error icon reference */
    ERROR("error"),
    /** the warn icon reference */
    WARN("warn"),
    /** the info icon reference */
    INFO("info"),
    /** the question icon reference */
    QUESTION("question");

    /** name of the enum */
    private String name;

    /**
     * Constructor for creating local enums.
     *
     * @param name name of the enum
     */
    GenericDialogType(String name) {
        this.name = name;
    }

    /**
     * gets the name of the enum
     */
    public String getName() {
        return name;
    }
}
