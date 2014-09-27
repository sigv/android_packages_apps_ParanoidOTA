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

package com.paranoid.paranoidota.updater;

import com.paranoid.paranoidota.IOUtils;
import com.paranoid.paranoidota.Version;

import java.io.Serializable;
import java.net.URL;

/**
 * The update package meta-data object.
 */
public class UpdatePackage implements Serializable {

    /** Device name to be used for Google Apps packages. */
    public static final String DEVICE_NAME_GAPPS = "-gapps-";

    /** The name of the device on which this package should be installed. */
    private final String mDeviceName;

    /** The version information. */
    private final Version mVersion;

    /** The name of this package's image file. */
    private final String mFilename;

    /** The size of this package's image file in bytes. */
    private final long mSizeBytes;

    /** The MD5 of this package's image file. */
    private final String mMd5;

    /** The URL which this package's image file can be downloaded from. */
    private final URL mUrl;

    /**
     * Initialize the package meta-data object.
     *
     * @param deviceName the name of the device on which this package should be installed
     * @param version the version information
     * @param filename the name of this package's image file
     * @param sizeBytes the size of this package's image file in bytes
     * @param md5 the MD5 of this package's image file
     * @param url the URL which this package's image file can be downloaded from
     */
    public UpdatePackage(final String deviceName, final Version version, final String filename,
            final long sizeBytes, final String md5, final URL url) {
        mDeviceName = deviceName;
        mVersion = version;
        mFilename = filename;
        mSizeBytes = sizeBytes;
        mMd5 = md5;
        mUrl = url;
    }

    /** @return the name of the device on which this package should be installed */
    public String getDeviceName() {
        return mDeviceName;
    }

    /** @return whether the package has been tagged to contain Google Apps */
    public boolean isGapps() {
        return mDeviceName.equals(DEVICE_NAME_GAPPS);
    }

    /** @return the version information */
    public Version getVersion() {
        return mVersion;
    }

    /** @return the name of this package's image file */
    public String getFilename() {
        return mFilename;
    }

    /** @return the size of this package's image file in bytes */
    public long getSizeBytes() {
        // IOUtils.formatHumanReadableByteCount(size, false);
        return mSizeBytes;
    }

    /** @return the MD5 of this package's image file */
    public String getMd5() {
        return mMd5;
    }

    /** @return the URL which this package's image file can be downloaded from */
    public URL getUrl() {
        return mUrl;
    }

}
