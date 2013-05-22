package de.dal33t.powerfolder.util.db;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * @author <a href="krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class URLUserType extends Loggable implements UserType {

    private static final int[] sqlTypes = {Types.VARCHAR};

    @Override
    public Object assemble(Serializable cached, Object owner)
        throws HibernateException
    {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
        throws HibernateException, SQLException
    {
        String urlString = rs.getString(names[0]);

        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new HibernateException(e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index)
        throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, value.toString());
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
        throws HibernateException
    {
        return original;
    }

    @Override
    public Class<?> returnedClass() {
        return URL.class;
    }

    @Override
    public int[] sqlTypes() {
        return sqlTypes;
    }
}
