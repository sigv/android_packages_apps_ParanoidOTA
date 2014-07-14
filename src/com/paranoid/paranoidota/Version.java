/*
 * Copyright 2014 ParanoidAndroid Project
 *
 * This file is part of Paranoid OTA.
 *
 * Paranoid OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Paranoid OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Paranoid OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paranoid.paranoidota;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version meta-data object. This implementation follows the Semantic
 * Versioning Specification (SemVer) 2.0.0.
 */
public final class Version implements Comparable<Version> {

    /** Internal PA specification helper wrapping some of the parsing. */
    private static class PASupport {

        /** @hide */
        private static final String ID_TRAILING_ALPHA = "1";

        /** @hide */
        private static final String ID_TRAILING_BETA = "2";

        /** @hide */
        private static final String ID_TRAILING_GOLD = "4";

        /** @hide */
        private static final String ID_TRAILING_PHASE_MARKER = "phase";

        /** @hide */
        private static final String ID_TRAILING_RC = "3";

        /**
         * Parses trailing bits of the PA specification so that they can be used
         * for a SemVer-style object as the pre-release version.
         * 
         * @param input the trailing bits of the PA specification
         * @return the array to use as a part of the pre-release version
         */
        private static String[] parseTrailingValue(String input) {
            if (input.startsWith("-")) {
                input = input.substring(1);
            }
            if (input.length() == 0) {
                // nothing specified
                return new String[] {
                        "", ""
                };
            } else if (input.substring(0, 1).matches("[0-9]")) {
                // release date
                try {
                    return new String[] {
                            input.substring(0, 8), input.substring(8)
                    };
                } catch (final IndexOutOfBoundsException e) {
                    return new String[] {
                            input, ""
                    };
                }
            } else if (input.startsWith("A")) {
                // phase ALPHA X
                return new String[] {
                        ID_TRAILING_PHASE_MARKER, ID_TRAILING_ALPHA,
                        input.substring(input.startsWith("ALPHA") ? 5 : 1)
                };
            } else if (input.startsWith("B")) {
                // phase BETA X
                return new String[] {
                        ID_TRAILING_PHASE_MARKER, ID_TRAILING_BETA,
                        input.startsWith("BETA") ? input.length() > 4 ? input.substring(4) : "0"
                                : input.length() > 1 ? input.substring(1) : "0"
                };
            } else if (input.startsWith("RC")) {
                // phase RC X
                return new String[] {
                        ID_TRAILING_PHASE_MARKER, ID_TRAILING_RC,
                        input.length() > 2 ? input.substring(2) : "0"
                };
            } else {
                // unknown - assume phase GOLD 0
                return new String[] {
                        ID_TRAILING_PHASE_MARKER, ID_TRAILING_GOLD, "0"
                };
            }
        }

    }

    /** The semi-capturing group for the build meta-data. */
    private static final String PATTERN_BUILD_METADATA_GROUP;

    /** The semi-capturing group for the pre-release version. */
    private static final String PATTERN_PRE_RELEASE_VERSION_GROUP;

    /** Logging tag for the class. */
    private static final String TAG = "Version";

    static {
        /*
         * The capturing identifier group consists of unlimited count of ASCII
         * characters split in groups. A dot is used as a group seperator.
         */
        final String identifierGroup = "([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)";

        PATTERN_PRE_RELEASE_VERSION_GROUP = "(?:-" + identifierGroup + ")?";
        PATTERN_BUILD_METADATA_GROUP = "(?:\\+" + identifierGroup + ")?";
    }

    /**
     * Parses PA package version meta-data into a SemVer-style version object.
     * <p>
     * In general, PA follows <i>pa_A-B-C.DE-FG-H.zip</i> where:<br>
     * A - device name, string<br>
     * B - optional device extras, string<br>
     * C - major version number, integer<br>
     * D - minor version number, single digit<br>
     * E - optional maintenance version number, integer<br>
     * F - optional development phase, string<br>
     * G - optional development phase number, integer<br>
     * H - optional release date, string
     * <p>
     * When generating the SemVer object:<br>
     * The device name and extras are held separately and do not affect the
     * SemVer object. The major, minor and maintenance versions are used as
     * major, minor and patch versions respectively with the maintenance/patch
     * version defaulting to 0 if not specified. The pre-release version
     * consists of up to 4 identifiers. The first one is based on the
     * development phase string transformed to an integer to allow comparisons.
     * The development phase number, which defaults to 0 if not specified, is
     * the next identifier. The release date is then appended as the third
     * identifier. If the release date contains a padding letter, that is
     * stripped and added as the fourth identifier. Some of these identifiers
     * may remain empty and be stripped automatically.
     * 
     * @param version the PA package version meta-data
     * @return the parsed SemVer object
     * @throws IllegalArgumentException in case the input meta-data is invalid
     * @see #parseSafePA(String)
     */
    public static Version parsePA(final String version) throws IllegalArgumentException {
        final Matcher m = Pattern
                .compile(
                        "(?:pa_)?(.+(?:-.+)?)-(\\d+)\\.(\\d)(?:\\.?(\\d+))?(-\\w+)?(-\\w+)?(?:-signed)?(?:\\.zip)?")
                .matcher(version);

        if (!m.find(0)) {
            throw new IllegalArgumentException("PA specification unmatched for " + version + ".");
        }

        if (m.start() != 0 || m.end() != version.length()) {
            throw new IllegalArgumentException("PA specification with unexpected padding for "
                    + version + ".");
        }

        try {
            // m.group(1) is the device name

            final int majorVersion = Integer.parseInt(m.group(2), 10);
            final int minorVersion = Integer.parseInt(m.group(3), 10);
            final int maintenanceVersion = Integer.parseInt(swapNull(m.group(4), "0"), 10);

            String[] trailingOne = PASupport.parseTrailingValue(swapNull(m.group(5), ""));
            String[] trailingTwo = PASupport.parseTrailingValue(swapNull(m.group(6), ""));

            if (trailingOne[0].equals(PASupport.ID_TRAILING_PHASE_MARKER)) {
                final String[] temporaryStore = trailingOne;
                trailingOne = trailingTwo;
                trailingTwo = temporaryStore;
            }

            if (trailingOne.length < 2 || trailingTwo.length < 2) {
                throw new IllegalArgumentException(
                        "PA specification hit two unfilled trailing values for " + version + ".");
            }

            final ArrayList<String> identifiers = new ArrayList<String>();

            for (final String identifier : new String[] {
                    trailingOne[trailingOne.length - 2], trailingOne[trailingOne.length - 1],
                    trailingTwo[trailingTwo.length - 2], trailingTwo[trailingTwo.length - 1]
            }) {
                if (identifier.length() > 0) {
                    identifiers.add(identifier);
                }
            }

            return new Version(majorVersion, minorVersion, maintenanceVersion,
                    identifiers.toArray(new String[identifiers.size()]), new String[] {});
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("PA specification hit an unexpected non-number for "
                    + version + ".");
        }
    }

    /**
     * Parses a PA package version meta-data into a SemVer-style version object.
     * Invalid input meta-data will simply return an SemVer object with reset
     * fields.
     * 
     * @param version the PA package version meta-data
     * @return the parsed SemVer object
     * @see #parsePA(String)
     */
    public static Version parseSafePA(final String version) {
        try {
            return parsePA(version);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Returning a safe version object for the PA package.", e);
            return new Version(0, 0, 0);
        }
    }

    /**
     * Makes sure the string value used is not null. Perfect for usage when a
     * value can be returned as null, but can be substituted with another
     * default value such as in pattern matching when groups are skipped.
     * 
     * @param input the original string
     * @param def the string to use as default for input
     * @return the input string or the default string, if the input was null
     */
    private static String swapNull(final String input, final String def) {
        return input == null ? def : input;
    }

    /** The dot-separated identifiers added as build meta-data. */
    private final String[] mBuildMetadata;

    /** The major version number. */
    private final int mMajorVersion;

    /** The minor version number. */
    private final int mMinorVersion;

    /** The patch version number. */
    private final int mPatchVersion;

    /** The dot-separated identifiers added to pre-release versions. */
    private final String[] mPreReleaseVersion;

    /**
     * Initializes a version object.
     * 
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @param patchVersion the patch version number
     */
    public Version(final int majorVersion, final int minorVersion, final int patchVersion) {
        this(majorVersion, minorVersion, patchVersion, new String[] {}, new String[] {});
    }

    /**
     * Initializes a version object.
     * 
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @param patchVersion the patch version number
     * @param preReleaseVersion the dot-separated identifiers added to
     *            pre-release versions
     */
    public Version(final int majorVersion, final int minorVersion, final int patchVersion,
            final String[] preReleaseVersion) {
        this(majorVersion, minorVersion, patchVersion, preReleaseVersion, new String[] {});
    }

    /**
     * Initializes a version object.
     * 
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @param patchVersion the patch version number
     * @param preReleaseVersion the dot-separated identifiers added to
     *            pre-release versions
     * @param buildMetadata the dot-separated identifiers added as build
     *            meta-data
     */
    public Version(final int majorVersion, final int minorVersion, final int patchVersion,
            final String[] preReleaseVersion, final String[] buildMetadata) {
        mMajorVersion = majorVersion;
        mMinorVersion = minorVersion;
        mPatchVersion = patchVersion;

        for (final String identifier : preReleaseVersion) {
            if (identifier.length() == 0) {
                throw new IllegalArgumentException(
                        "Tried creating a version object containing a pre-release version identifier with the length of 0.");
            }
        }
        mPreReleaseVersion = preReleaseVersion;

        for (final String identifier : buildMetadata) {
            if (identifier.length() == 0) {
                throw new IllegalArgumentException(
                        "Tried creating a version object containing a build metadata identifier with the length of 0.");
            }
        }
        mBuildMetadata = buildMetadata;
    }

    /**
     * Initializes a version object.
     * 
     * @param version the version string to parse following SemVer
     */
    public Version(final String version) {
        final Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)"
                + PATTERN_PRE_RELEASE_VERSION_GROUP + PATTERN_BUILD_METADATA_GROUP);
        final Matcher m = p.matcher(version);

        if (!m.find(0)) {
            throw new IllegalArgumentException("SemVer unmatched for " + version + ".");
        }

        if (m.start() != 0 || m.end() != version.length()) {
            throw new IllegalArgumentException("SemVer with unexpected padding for " + version
                    + ".");
        }

        try {
            mMajorVersion = Integer.parseInt(m.group(1), 10);
            mMinorVersion = Integer.parseInt(m.group(2), 10);
            mPatchVersion = Integer.parseInt(m.group(3), 10);

            final ArrayList<String> preReleaseVersion = new ArrayList<String>();
            for (final String identifier : swapNull(m.group(4), "").split("\\.")) {
                if (identifier.length() > 0) {
                    preReleaseVersion.add(identifier);
                }
            }
            mPreReleaseVersion = preReleaseVersion.toArray(new String[preReleaseVersion.size()]);

            final ArrayList<String> buildMetadata = new ArrayList<String>();
            for (final String identifier : swapNull(m.group(5), "").split("\\.")) {
                if (identifier.length() > 0) {
                    buildMetadata.add(identifier);
                }
            }
            mBuildMetadata = buildMetadata.toArray(new String[buildMetadata.size()]);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("SemVer hit an unexpected non-number for "
                    + version + ".");
        }
    }

    @Override
    public int compareTo(final Version another) {
        final int major = another.getMajorVersion();
        if (mMajorVersion != major) {
            return mMajorVersion > major ? 1 : -1;
        }

        final int minor = another.getMinorVersion();
        if (mMinorVersion != minor) {
            return mMinorVersion > minor ? 1 : -1;
        }

        final int patch = another.getPatchVersion();
        if (mPatchVersion != patch) {
            return mPatchVersion > patch ? 1 : -1;
        }

        final String[] preReleaseVersion = another.getPreReleaseVersion();
        final int l = Math.max(mPreReleaseVersion.length, preReleaseVersion.length);
        for (int i = 0; i < l; i++) {
            // the pre-release identifiers for this version are shorter
            if (i >= mPreReleaseVersion.length) {
                return -1;
            }

            // the pre-release identifiers for the another version are shorter
            if (i >= preReleaseVersion.length) {
                return 1;
            }

            // the pre-release identifiers do not match at this location
            if (!mPreReleaseVersion[i].equals(preReleaseVersion[i])) {
                Integer thisIdentifierInt = null;
                try {
                    thisIdentifierInt = Integer.parseInt(mPreReleaseVersion[i]);
                } catch (final NumberFormatException e) {
                }

                Integer anotherIdentifierInt = null;
                try {
                    anotherIdentifierInt = Integer.parseInt(preReleaseVersion[i]);
                } catch (final NumberFormatException e) {
                }

                // both pre-release identifiers are integers
                if (thisIdentifierInt != null && anotherIdentifierInt != null) {
                    return thisIdentifierInt > anotherIdentifierInt ? 1
                            : -1;
                }

                // this pre-release identifier is an int while another isn't
                if (thisIdentifierInt != null && anotherIdentifierInt == null) {
                    return -1;
                }

                // this pre-release identifier is not int while another is
                if (thisIdentifierInt == null && anotherIdentifierInt != null) {
                    return 1;
                }

                // neither pre-release identifier is an int
                return mPreReleaseVersion[i].compareTo(preReleaseVersion[i]);
            }
        }

        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        // The objects are identical. (just for optimization)
        if (this == o) {
            return true;
        }

        // The other object has the wrong type.
        if (!(o instanceof Version)) {
            return false;
        }

        // Cast and check all the fields.
        final Version v = (Version) o;
        return mMajorVersion == v.mMajorVersion && mMinorVersion == v.mMinorVersion
                && mPatchVersion == v.mPatchVersion
                && Arrays.equals(mPreReleaseVersion, v.mPreReleaseVersion);
    }

    /** @return the dot-separated identifiers added as build meta-data */
    public String[] getBuildMetadata() {
        return mBuildMetadata;
    }

    /** @return the major version number */
    public int getMajorVersion() {
        return mMajorVersion;
    }

    /** @return the minor version number */
    public int getMinorVersion() {
        return mMinorVersion;
    }

    /** @return the patch version number */
    public int getPatchVersion() {
        return mPatchVersion;
    }

    /** @return the dot-separated identifiers added to pre-release versions */
    public String[] getPreReleaseVersion() {
        return mPreReleaseVersion;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param another the other version to with the check should be done
     * @return true if this package is newer than or equal to the version
     *         provided for comparison
     */
    public boolean isNewerThanOrEqualTo(final Version another) {
        // compareTo(another) > 0 means this is newer
        // compareTo(another) == 0 means they match
        return compareTo(another) >= 0;
    }

    /**
     * Formats the version object for displaying to the user. This method should
     * not be depended upon to return consistent values across multiple versions
     * of the code (which would be helpful for later parsing) and should only be
     * used for displaying at runtime.
     * 
     * @return a formatted string to display to the user
     */
    public String toDisplayString() {
        String out = mMajorVersion + "." + mMinorVersion + "." + mPatchVersion;

        if (mPreReleaseVersion.length > 0) {
            out += " (";
            for (final String part : mPreReleaseVersion) {
                out += part + " ";
            }
            out = out.trim() + ")";
        }

        if (mBuildMetadata.length > 0) {
            out += " [";
            for (final String part : mBuildMetadata) {
                out += part + " ";
            }
            out = out.trim() + "]";
        }

        return out;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + "majorVersion=" + mMajorVersion + ", "
                + "minorVersion=" + mMinorVersion + ", " + "patchVersion=" + mPatchVersion + ", "
                + "preReleaseVersion=" + Arrays.toString(mPreReleaseVersion) + ", "
                + "buildMetadata=" + Arrays.toString(mBuildMetadata) + "]";
    }

}
