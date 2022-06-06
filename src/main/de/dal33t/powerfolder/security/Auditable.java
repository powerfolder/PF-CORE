package de.dal33t.powerfolder.security;

public interface Auditable {
    void setCreatedNowBy(final Account caller);
    void setModifiedNowBy(final Account caller);
}
