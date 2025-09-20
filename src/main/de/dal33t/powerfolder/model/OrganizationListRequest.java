package de.dal33t.powerfolder.model;

public class OrganizationListRequest {
    private final String query;
    private final String adminAccountOID;
    private final String adminAccountUsername;
    private final String migrationStatus;
    private final String page;
    private final String size;

    private OrganizationListRequest(Builder b) {
        this.query = b.query;
        this.adminAccountOID = b.adminAccountOID;
        this.adminAccountUsername = b.adminAccountUsername;
        this.migrationStatus = b.migrationStatus;
        this.page = b.page;
        this.size = b.size;
    }

    public String getQuery()               { return query; }
    public String getAdminAccountOID()     { return adminAccountOID; }
    public String getAdminAccountUsername(){ return adminAccountUsername; }
    public String getMigrationStatus()     { return migrationStatus; }
    public String getPage()                { return page; }
    public String getSize()                { return size; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String query;
        private String adminAccountOID;
        private String adminAccountUsername;
        private String migrationStatus;
        private String page;
        private String size;

        public Builder query(String v)                { this.query = v; return this; }
        public Builder adminAccountOID(String v)      { this.adminAccountOID = v; return this; }
        public Builder adminAccountUsername(String v) { this.adminAccountUsername = v; return this; }
        public Builder migrationStatus(String v)      { this.migrationStatus = v; return this; }
        public Builder page(String v)                 { this.page = v; return this; }
        public Builder size(String v)                 { this.size = v; return this; }
        public OrganizationListRequest build()        { return new OrganizationListRequest(this); }
    }
}
