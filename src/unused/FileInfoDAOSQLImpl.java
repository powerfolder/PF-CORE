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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * DAO for {@link FileInfo}s stored in a relational database
 * 
 * @author sprajc
 */
public class FileInfoDAOSQLImpl extends PFComponent implements FileInfoDAO {
    private static final String DEFAULT_TABLE_NAME = "FileInfo";

    // SQLs
    public static final String SQL_INSERT = "INSERT INTO FileInfo VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_COUNT = "SELECT COUNT(fileName) FROM %TABLE_NAME% WHERE domain = ?";
    private static final String SQL_FIND_ALL_FILES = "SELECT * FROM %TABLE_NAME% WHERE domain = ? AND dir = false";
    private static final String SQL_FIND_ALL_DIRS = "SELECT * FROM %TABLE_NAME% WHERE domain = ? AND dir = true";
    private static final String SQL_FIND = "SELECT * FROM %TABLE_NAME% WHERE %FILE_NAME_FIELD% = ? AND domain = ?";
    private static final String SQL_FIND_NEWEST_VERSION = "SELECT * FROM %TABLE_NAME% WHERE %FILE_NAME_FIELD% = ? AND domain IN (?) ORDER BY version DESC";
    private static final String SQL_DELETE = "DELETE FROM %TABLE_NAME% WHERE %FILE_NAME_FIELD% = ? AND domain = ?";
    private static final String SQL_DELETE_DOMAIN = "DELETE FROM %TABLE_NAME% WHERE domain = ?";

    private String tableName;
    private final JdbcConnectionPool connectionPool;
    private boolean ignoreFileNameCase;

    public FileInfoDAOSQLImpl(Controller controller, String dbURL,
        String username, String password, String tableName)
    {
        super(controller);
        Reject.ifBlank(dbURL, "Database URL is blank");
        if (StringUtils.isBlank(tableName)) {
            this.tableName = DEFAULT_TABLE_NAME;
        } else {
            this.tableName = tableName;
        }
        // TODO Configure pool
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(dbURL);
        ds.setUser(username);
        ds.setPassword(password);
        this.connectionPool = JdbcConnectionPool.create(ds);
        connectionPool.setMaxConnections(1000);
        connectionPool.setLoginTimeout(1);
        ignoreFileNameCase = OSUtil.isWindowsSystem();
        // ignoreFileNameCase = false;

        try {
            byte[] content = StreamUtils.readIntoByteArray(Thread
                .currentThread().getContextClassLoader().getResourceAsStream(
                    "sql/create_folder_table.sql"));
            String createTableSQL = new String(content, "UTF-8").replace(
                "%TABLE_NAME%", "FileInfo");
            executeSQL(createTableSQL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (connectionPool != null) {
            try {
                connectionPool.dispose();
            } catch (SQLException e) {
                logSevere("Problem closing database connection pool. " + e, e);
            }
        }
    }

    public int count(String domain) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = c.prepareStatement(tn(SQL_COUNT));
            ps.setString(1, dn(domain));
            ps.execute();
            ResultSet rs = ps.getResultSet();
            rs.next();
            int count = rs.getInt(1);
            ps.close();
            return count;
        } catch (SQLException e) {
            throw handleSQLException(e, tn(SQL_COUNT));
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    public void delete(String domain, FileInfo info) {
        Connection c = openConnection();
        try {
            delete0(domain, info, c);
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    private void delete0(String domain, FileInfo info, Connection c) {
        try {
            PreparedStatement ps = createCaseQuery(c, SQL_DELETE, info
                .getRelativeName());
            ps.setString(2, dn(domain));
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            throw handleSQLException(e, SQL_DELETE);
        }
    }

    public void deleteDomain(String domain) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = c.prepareStatement(tn(SQL_DELETE_DOMAIN));
            ps.setString(1, dn(domain));
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            throw handleSQLException(e, tn(SQL_DELETE_DOMAIN));
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    public FileInfo find(FileInfo info, String domain) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = createCaseQuery(c, SQL_FIND, info
                .getRelativeName());
            ps.setString(2, dn(domain));
            ps.execute();
            ResultSet rs = ps.getResultSet();
            if (!rs.next()) {
                return null;
            }
            FileInfo fInfo = FileInfoSQLConverter.get(getController(), rs);
            if (rs.next()) {
                logSevere("Found multiple FileInfos in domain '" + domain
                    + "': " + info.toDetailString());
            }
            ps.close();
            return fInfo;
        } catch (SQLException e) {
            throw handleSQLException(e, SQL_FIND);
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    public Collection<FileInfo> findAllFiles(String domain) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = c.prepareStatement(tn(SQL_FIND_ALL_FILES));
            ps.setString(1, dn(domain));
            ps.execute();
            ResultSet rs = ps.getResultSet();
            // TODO Optimize. Initialize with size.
            Map<FileInfo, FileInfo> fInfos = new HashMap<FileInfo, FileInfo>();
            while (rs.next()) {
                FileInfo fInfo = FileInfoSQLConverter.get(getController(), rs);
                if (fInfos.put(fInfo, fInfo) != null) {
                    logSevere("Found multiple FileInfos in domain '" + domain
                        + "': " + fInfo.toDetailString());
                }
            }
            ps.close();
            return Collections.unmodifiableCollection(fInfos.values());
        } catch (SQLException e) {
            throw handleSQLException(e, tn(SQL_FIND_ALL_FILES));
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    private RuntimeException handleSQLException(SQLException e, String sql) {
        logSevere("Unable to execute database query: " + sql, e);
        throw new RuntimeException("Unable to execute database query: " + sql,
            e);
    }

    public FileInfo findNewestVersion(FileInfo info, String... domains) {
        Connection c = openConnection();
        try {
            // FIXME use prepared / static query
            StringBuilder q = new StringBuilder(
                "SELECT * FROM %TABLE_NAME% WHERE %FILE_NAME_FIELD% = ? AND (1=0 ");

            for (String domain : domains) {
                q.append("OR domain = '");
                q.append(dn(domain));
                q.append("'");
            }

            q.append(") ORDER BY version DESC");
            PreparedStatement ps = createCaseQuery(c, q.toString(), info
                .getRelativeName());

            // ps.setString(2, b.toString());
            ps.execute();
            ResultSet rs = ps.getResultSet();
            if (!rs.next()) {
                return null;
            }
            FileInfo fInfo = FileInfoSQLConverter.get(getController(), rs);
            // if (rs.next()) {
            // logSevere("Found multiple FileInfos in domain '" + domain
            // + "': " + info.toDetailString());
            // }
            ps.close();
            return fInfo;
        } catch (SQLException e) {
            throw handleSQLException(e, SQL_FIND_NEWEST_VERSION);
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    public void store(String domain, FileInfo... infos) {
        store(domain, Arrays.asList(infos));
    }

    public void store(String domain, Collection<FileInfo> infos) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = c.prepareStatement(tn(SQL_INSERT));
            ps.setString(1, dn(domain));
            for (FileInfo fInfo : infos) {
                delete0(domain, fInfo, c);
                FileInfoSQLConverter.set(fInfo, ps);
                ps.execute();
            }
            ps.close();
        } catch (SQLException e) {
            logSevere("Unable to execute database query: " + tn(SQL_COUNT), e);
            throw new RuntimeException("Unable to execute database query: "
                + tn(SQL_COUNT), e);
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    // Non implementing methods ***********************************************

    public void executeSQL(String sql) {
        Connection c = openConnection();
        try {
            Statement s = c.createStatement();
            s.execute(tn(sql));
            s.close();
        } catch (SQLException e) {
            logSevere("Unable to execute database query: " + tn(sql), e);
            throw new RuntimeException("Unable to execute database query: "
                + tn(sql), e);
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    // Internal methods *******************************************************

    private Connection openConnection() {
        try {
            int ac = connectionPool.getActiveConnections();
            if (ac > 500) {
                logWarning("Active connection: "
                    + connectionPool.getActiveConnections(),
                    new RuntimeException("here"));
            }

            return connectionPool.getConnection();
        } catch (SQLException e) {
            logSevere("Unable to open database connection. " + e, e);
            throw new RuntimeException("Unable to open database connection. "
                + e, e);
        }
    }

    private String tn(String rawSQL) {
        return rawSQL.replace("%TABLE_NAME%", tableName);
    }

    private String dn(String domain) {
        if (StringUtils.isBlank(domain)) {
            return "XX";
        }
        return domain;
    }

    private String caseSenstive(String sql) {
        return sql.replace("%FILE_NAME_FIELD%", "fileName");
    }

    private String caseInsenstive(String sql) {
        return sql.replace("%FILE_NAME_FIELD%", "fileNameLower");
    }

    private PreparedStatement createCaseQuery(Connection c, String sql,
        String fileName) throws SQLException
    {
        PreparedStatement ps;
        if (ignoreFileNameCase) {
            ps = c.prepareStatement(tn(caseInsenstive(sql)));
            ps.setString(1, fileName.toLowerCase());
        } else {
            ps = c.prepareStatement(tn(caseSenstive(sql)));
            ps.setString(1, fileName);
        }
        return ps;
    }

    public Iterator<FileInfo> findDifferentFiles(int maxResults,
        String... domains)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    public FileHistory getFileHistory(FileInfo fileInfo) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Collection<DirectoryInfo> findAllDirectories(String domain) {
        Connection c = openConnection();
        try {
            PreparedStatement ps = c.prepareStatement(tn(SQL_FIND_ALL_DIRS));
            ps.setString(1, dn(domain));
            ps.execute();
            ResultSet rs = ps.getResultSet();
            // TODO Optimize. Initialize with size.
            Map<DirectoryInfo, DirectoryInfo> fInfos = new HashMap<DirectoryInfo, DirectoryInfo>();
            while (rs.next()) {
                DirectoryInfo dirInfo = (DirectoryInfo) FileInfoSQLConverter
                    .get(getController(), rs);
                if (fInfos.put(dirInfo, dirInfo) != null) {
                    logSevere("Found multiple FileInfos in domain '" + domain
                        + "': " + dirInfo.toDetailString());
                }
            }
            ps.close();
            return Collections.unmodifiableCollection(fInfos.values());
        } catch (SQLException e) {
            throw handleSQLException(e, tn(SQL_FIND_ALL_DIRS));
        } finally {
            try {
                c.close();
            } catch (SQLException e) {
                logWarning("Unable to close database connection", e);
            }
        }
    }

    public Collection<FileInfo> findInDirectory(String path, String domain,
        boolean recursive)
    {
        throw new UnsupportedOperationException("Not implemented");
    }
}
