package de.dal33t.powerfolder.util.ui;

public enum GenericDialogType {

    DEFAULT("default"),
    ERROR("error"),
    WARN("warn"),
    INFO("info"),
    QUESTION("question");

    private String name;

    GenericDialogType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
