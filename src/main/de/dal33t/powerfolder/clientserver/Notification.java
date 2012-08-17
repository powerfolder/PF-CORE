/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: FileChunk.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.clientserver;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Generic notification to Account / User.
 * 
 * @author sprajc
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Notification implements Serializable {

    private static final long serialVersionUID = 100L;

    // Properties
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_CREATION_DATE = "creationDate";
    public static final String PROPERTYNAME_EMAIL_SENT_DATE = "emailSentDate";
    public static final String PROPERTYNAME_CLIENT_RECEIVED_DATE = "clientReceivedDate";
    public static final String PROPERTYNAME_ACCOUNT_OID = "accountOID";
    public static final String PROPERTYNAME_TYPE_ID = "typeId";
    public static final String PROPERTYNAME_SUBJECT = "subject";
    public static final String PROPERTYNAME_SUBJECT_TRANSLATION_ID = "subjectTranslationID";
    public static final String PROPERTYNAME_TEXT = "text";
    public static final String PROPERTYNAME_TEXT_TRANSLATION_ID = "textTranslationID";

    /**
     * Absolutely unique ID.
     */
    @Id
    private String oid;

    /**
     * The date this notification was created.
     */
    private Date creationDate;

    /**
     * The date this notification was sent by email.
     */
    private Date emailSentDate;

    /**
     * The date this notification was received and read in the client software.
     */
    private Date clientReceivedDate;

    /**
     * The OID of the target Account/User.
     */
    @Index(name = "IDX_NOT_ACCOUNT_OID")
    private String accountOID;

    /**
     * A logical id for this type of notification. e.g.
     * "PROD_EXP_FINWARN_LICENSE_O-MP-2007-01-01-FDJHFDJ" for the final warning
     * about the expired license purchased on 1.1.2007. Use to re-identify
     * already created notification.
     */
    @Index(name = "IDX_TYPE_ID")
    private String typeId;

    /**
     * The summary/subject for this message.
     */
    private String subject;

    /**
     * Optional: Translation id to retrieve the summary/subject for this message
     * in different languages.
     */
    private String subjectTranslationID;

    /**
     * The text of this notification in English language.
     */
    private String text;

    /**
     * Optional: The translation id to retrieve the text of this notification in
     * different languages.
     */
    private String textTranslationID;

    Notification() {
        // NOP - for hibernate
    }

    public Notification(String typeId) {
        Reject.ifBlank(typeId, "typeId id is blank");
        this.typeId = typeId;
        this.creationDate = new Date();
        this.oid = IdGenerator.makeId();
    }

    public String getOID() {
        return oid;
    }

    public String getTypeId() {
        return typeId;
    }

    /**
     * @param id
     * @deprecated ONLY FOR MIGRATION!!
     */
    public void setTypeId(String id) {
        this.typeId = id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public boolean isEmailSent() {
        return emailSentDate != null;
    }

    public Date getEmailSentDate() {
        return emailSentDate;
    }

    public void setEmailSentDate(Date emailSentDate) {
        this.emailSentDate = emailSentDate;
    }

    public boolean isClientReceived() {
        return clientReceivedDate != null;
    }

    public Date getClientReceivedDate() {
        return clientReceivedDate;
    }

    public void setClientReceivedDate(Date clientReceivedDate) {
        this.clientReceivedDate = clientReceivedDate;
    }

    public String getAccountOID() {
        return accountOID;
    }

    public void setAccountOID(String accountOID) {
        this.accountOID = accountOID;
    }

    /**
     * Save get subject. Try i18ned version. Take plain subject if not found.
     * 
     * @return the subject
     */
    public String getSubjectI15D() {
        String i15d = null;
        if (StringUtils.isNotBlank(subjectTranslationID)) {
            i15d = Translation.getTranslation(subjectTranslationID);
        }
        // Unknown, empty translation or non i15d
        if (StringUtils.isBlank(i15d) || i15d.startsWith(" -")) {
            i15d = subject;
        }
        return i15d;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubjectTranslationID() {
        return subjectTranslationID;
    }

    public void setSubjectTranslationID(String subjectTranslationID) {
        this.subjectTranslationID = subjectTranslationID;
    }

    /**
     * Save get text. Try i18ned version. Take text if not found.
     * 
     * @return the subject
     */
    public String getTextI15D() {
        String i15d = null;
        if (StringUtils.isNotBlank(textTranslationID)) {
            i15d = Translation.getTranslation(textTranslationID);
        }
        // Unknown, empty translation or non i15d
        if (StringUtils.isBlank(i15d) || i15d.startsWith(" -")) {
            i15d = text;
        }
        return i15d;
    }

    public String getTextTranslationID() {
        return textTranslationID;
    }

    public void setTextTranslationID(String textTranslationID) {
        this.textTranslationID = textTranslationID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Notification other = (Notification) obj;
        if (oid == null) {
            if (other.oid != null)
                return false;
        } else if (!oid.equals(other.oid))
            return false;
        return true;
    }
}
