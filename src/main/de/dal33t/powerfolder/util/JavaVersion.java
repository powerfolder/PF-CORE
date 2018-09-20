/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: JavaVersion.java 6419 2009-01-19 01:03:34Z tot $
 */
package de.dal33t.powerfolder.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a Java version. It follows the java.runtime.version format
 * of &lt;major&gt;.&lt;minor&gt;.&lt;revision&gt;_&lt;update&gt;-b&lt;build&gt;,
 * eg 1.6.2_10-b12 It implements Comparable&lt;JavaVersion&gt; by traversing the
 * version values.
 */
public class JavaVersion implements Comparable<JavaVersion> {

    private static JavaVersion systemVersion;

    private final int major;
    private final int minor;
    private final int revision;
    private final int update;
    private final int build;

    /**
     * Constructor
     *
     * @param major
     * @param minor
     * @param revision
     * @param update
     * @param build
     */
    public JavaVersion(int major, int minor, int revision, int update, int build) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.update = update;
        this.build = build;
    }

    /**
     * Constructor, defaulting build to zero.
     *
     * @param major
     * @param minor
     * @param revision
     * @param update
     */
    public JavaVersion(int major, int minor, int revision, int update) {
        this(major, minor, revision, update, 0);
    }

    /**
     * Constructor, defaulting update and build to zero.
     *
     * @param major
     * @param minor
     * @param revision
     */
    public JavaVersion(int major, int minor, int revision) {
        this(major, minor, revision, 0, 0);
    }

    /**
     * Constructor, defaulting revision, update and build to zero.
     *
     * @param major
     * @param minor
     */
    public JavaVersion(int major, int minor) {
        this(major, minor, 0, 0, 0);
    }

    /**
     * Returns the build value of the version.
     *
     * @return
     */
    public int getBuild() {
        return build;
    }

    /**
     * Returns the major value of the version.
     *
     * @return
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor value of the version.
     *
     * @return
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the revision value of the version.
     *
     * @return
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Returns the update value of the version.
     *
     * @return
     */
    public int getUpdate() {
        return update;
    }

    /**
     * Tests for equality with another object. If object is a JavaVersion, tests
     * all version values.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        JavaVersion that = (JavaVersion) obj;

        if (build != that.build) {
            return false;
        }
        if (major != that.major) {
            return false;
        }
        if (minor != that.minor) {
            return false;
        }
        if (revision != that.revision) {
            return false;
        }
        if (update != that.update) {
            return false;
        }

        return true;
    }

    /**
     * Hash of the version values.
     *
     * @return
     */
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + revision;
        result = 31 * result + build;
        result = 31 * result + update;
        return result;
    }

    /**
     * Compare to another JavaVersion, progressing down major, minor, revision,
     * update and finally build.
     *
     * @param o
     * @return
     */
    public int compareTo(JavaVersion o) {
        if (major == o.major) {
            if (minor == o.minor) {
                if (revision == o.revision) {
                    if (update == o.update) {
                        if (build == o.build) {
                            return 0;
                        } else {
                            return build - o.build;
                        }
                    } else {
                        return update - o.update;
                    }
                } else {
                    return revision - o.revision;
                }
            } else {
                return minor - o.minor;
            }
        } else {
            return major - o.major;
        }
    }

    /**
     * Displays as
     * &lt;major&gt;.&lt;minor&gt;.&lt;revision&gt;_&lt;update&gt;-b&lt;build&gt;
     * It skips 'build' and 'update' values if not available (zero).
     *
     * @return
     */
    public String toString() {
        if (update > 0) {
            if (build > 0) {
                if (build <= 10) {
                    // build like '-b0x'
                    return major + "." + minor + '.' + revision + '_' + update
                        + "-b0" + build;
                } else {
                    return major + "." + minor + '.' + revision + '_' + update
                        + "-b" + build;
                }
            } else {
                return major + "." + minor + '.' + revision + '_' + update;
            }
        } else {
            return major + "." + minor + '.' + revision;
        }
    }

    /**
     * @return Is the installed JRE the OpenJDK-JRE
     */
    public boolean isOpenJDK() {
        String vendor = System.getProperty("java.vm.name");

        if (vendor.contains("OpenJDK")) {
            return true;
        }

        return false;
    }

    /**
     * Gets the system version of Java. First tries 'java.runtime.version', then
     * 'java.version', then 'java.specification.version', otherwise it folds.
     *
     * @return
     */
    public static JavaVersion systemVersion() {
        if (systemVersion == null) {
            String versionText = System.getProperty("java.runtime.version");
            if (versionText == null) {
                versionText = System.getProperty("java.version");
            }
            if (versionText == null) {
                versionText = System.getProperty("java.specification.version");
            }
            if (versionText == null) {
                // Unknown
                systemVersion = new JavaVersion(0, 0);
                return systemVersion;
                // throw new IllegalStateException(
                // "Could not retrieve the system version of Java.");
            }
            try {
                systemVersion = parse(versionText);
            } catch (Exception e) {
                systemVersion = new JavaVersion(0, 0);
            }

        }

        return systemVersion;
    }

    /**
     * Parse a string version into a Java Version. Expects something in between
     * '1.6' and '1.6.2_10-b12' format.
     *
     * @param version
     * @return
     */
    public static JavaVersion parse(String version) {
        String[] strings = version.split("\\.");
        if (strings.length < 2) {
            throw new IllegalStateException(
                "Could not parse system version of Java: " + version);
        }

        Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)[_\\+]?(\\d+)[\\-b(\\d+)]?");
        Matcher m = p.matcher(version);
        String build = "0", update = "0", revision = "0", minor = "0", major = "0";

        if (m.find()) {
            int count = m.groupCount();

            switch (count) {
                case 5:
                    build = m.group(5);
                case 4:
                    update = m.group(4);
                case 3:
                    revision = m.group(3);
                case 2:
                    minor = m.group(2);
                case 1:
                    major = m.group(1);
            }
        }

        return new JavaVersion(Integer.parseInt(major), Integer
            .parseInt(minor), Integer.parseInt(revision), Integer
            .parseInt(update), Integer.parseInt(build));
    }
}
