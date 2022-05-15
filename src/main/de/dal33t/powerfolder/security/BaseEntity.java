package de.dal33t.powerfolder.security;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Date;

@MappedSuperclass
public class BaseEntity {
    private Date creationDate;
    @Column(name = "account_oid")
    private String creationAccount ;
    private Date modifiedDate ;
    private String modifiedAccount ;

    public BaseEntity() {
        this.creationDate = new Date();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreationAccount() {
        return creationAccount;
    }

    public void setCreationAccount(String creationAccount) {
        this.creationAccount = creationAccount;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedAccount() {
        return modifiedAccount;
    }

    public void setModifiedAccount(String modifiedAccount) {
        this.modifiedAccount = modifiedAccount;
    }
}
