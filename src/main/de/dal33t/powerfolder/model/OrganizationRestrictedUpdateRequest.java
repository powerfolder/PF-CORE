package de.dal33t.powerfolder.model;


public final class OrganizationRestrictedUpdateRequest {

    private final String notes;
    private final String color1;
    private final String color2;
    private final String color3;

    private OrganizationRestrictedUpdateRequest(Builder b) {
        this.notes = b.notes;
        this.color1 = b.color1;
        this.color2 = b.color2;
        this.color3 = b.color3;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String notes;
        private String color1;
        private String color2;
        private String color3;

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder color1(String color1) {
            this.color1 = color1;
            return this;
        }

        public Builder color2(String color2) {
            this.color2 = color2;
            return this;
        }

        public Builder color3(String color3) {
            this.color3 = color3;
            return this;
        }

        public OrganizationRestrictedUpdateRequest build() {
            return new OrganizationRestrictedUpdateRequest(this);
        }
    }

    public String getNotes()  { return notes; }
    public String getColor1() { return color1; }
    public String getColor2() { return color2; }
    public String getColor3() { return color3; }


}
