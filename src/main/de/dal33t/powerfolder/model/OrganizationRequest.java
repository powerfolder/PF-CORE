package de.dal33t.powerfolder.model;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public final class OrganizationRequest {
    private final String id;
    private final String name;
    private final String notes;
    private final String custom1;
    private final String custom2;
    private final String custom3;
    private final String color1;
    private final String color2;
    private final String color3;
    private final Integer maxUsers;
    private final Long storageBytes;
    private final String ldapDN;
    private final String basePath;
    private final Date validFrom;
    private final Date validTill;
    private final Boolean isRestrictedToDomain;
    private final List<String> domains;
    private final JSONObject jsonObject;

    private OrganizationRequest(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.notes = builder.notes;
        this.custom1 = builder.custom1;
        this.custom2 = builder.custom2;
        this.custom3 = builder.custom3;
        this.color1 = builder.color1;
        this.color2 = builder.color2;
        this.color3 = builder.color3;
        this.maxUsers = builder.maxUsers;
        this.storageBytes = builder.storageBytes;
        this.ldapDN = builder.ldapDN;
        this.basePath = builder.basePath;
        this.validFrom = builder.validFrom;
        this.validTill = builder.validTill;
        this.isRestrictedToDomain = builder.isRestrictedToDomain;
        this.domains = builder.domains;
        this.jsonObject = builder.jsonObject;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public String getCustom1() {
        return custom1;
    }

    public String getCustom2() {
        return custom2;
    }

    public String getCustom3() {
        return custom3;
    }

    public String getColor1() {
        return color1;
    }

    public String getColor2() {
        return color2;
    }

    public String getColor3() {
        return color3;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public Long getStorageBytes() {
        return storageBytes;
    }

    public String getLdapDN() {
        return ldapDN;
    }

    public String getBasePath() {
        return basePath;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public Date getValidTill() {
        return validTill;
    }

    public Boolean isRestrictedToDomain() {return isRestrictedToDomain;}

    public List<String> getDomains() {return domains;}

    public JSONObject getJsonObject() {return jsonObject;}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String notes;
        private String custom1;
        private String custom2;
        private String custom3;
        private String color1;
        private String color2;
        private String color3;
        private Integer maxUsers;
        private Long storageBytes;
        private String ldapDN;
        private String basePath;
        private Date validFrom;
        private Date validTill;
        private Boolean isRestrictedToDomain;
        private List<String> domains;
        private JSONObject jsonObject;

        private Builder() {
        }

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder name(String v) {
            this.name = v;
            return this;
        }

        public Builder notes(String v) {
            this.notes = v;
            return this;
        }

        public Builder custom1(String v) {
            this.custom1 = v;
            return this;
        }

        public Builder custom2(String v) {
            this.custom2 = v;
            return this;
        }

        public Builder custom3(String v) {
            this.custom3 = v;
            return this;
        }

        public Builder color1(String v) {
            this.color1 = v;
            return this;
        }

        public Builder color2(String v) {
            this.color2 = v;
            return this;
        }

        public Builder color3(String v) {
            this.color3 = v;
            return this;
        }

        public Builder maxUsers(Integer v) {
            this.maxUsers = v;
            return this;
        }

        public Builder storageBytes(Long v) {
            this.storageBytes = v;
            return this;
        }

        public Builder ldapDN(String v) {
            this.ldapDN = v;
            return this;
        }

        public Builder basePath(String v) {
            this.basePath = v;
            return this;
        }

        public Builder validFrom(Date v) {
            this.validFrom = v;
            return this;
        }

        public Builder validTill(Date v) {
            this.validTill = v;
            return this;
        }

        public Builder isRestrictedToDomain(Boolean v){
            this.isRestrictedToDomain = v;
            return this;
        }

        public Builder domains(List<String> v) {
            this.domains = v;
            return this;
        }

        public Builder jsonObject(JSONObject v) {
            this.jsonObject = v;
            return this;
        }

        public OrganizationRequest build() {
            return new OrganizationRequest(this);
        }

    }
}
