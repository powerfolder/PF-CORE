package de.dal33t.powerfolder.util.db;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
 */
public class InetSocketAddressUserType extends Loggable implements UserType {
    private static final int[] SQL_TYPES =  { Types.VARCHAR };

    public InetSocketAddressUserType() {
        super();
    }
    
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        else if (x == null || y == null) {
            return false;
        }
        else {
            return x.equals(y);
        }
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {

        String addressAsString = rs.getString(names[0]);

        if (StringUtils.isBlank(addressAsString)) {
            return null;
        }

        String[] addAndPort = addressAsString.split(":");

        if (addAndPort.length != 2) {
            return null;
        }
        else {
            return new InetSocketAddress(addAndPort[0], Integer.valueOf(addAndPort[1]).intValue());
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        }
        else {
            InetSocketAddress address = (InetSocketAddress) value;
            String stringRepresentation = NetworkUtil.getHostAddressNoResolve(address.getAddress()) + ":" + address.getPort();
            st.setString(index, stringRepresentation);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class<?> returnedClass() {
        return InetSocketAddress.class;
    }

    public int[] sqlTypes() {
        return SQL_TYPES;
    }
}
