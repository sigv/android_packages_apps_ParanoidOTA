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

package com.paranoid.paranoidota.updater.server;

import com.paranoid.paranoidota.Version;
import com.paranoid.paranoidota.updater.UpdatePackage;

import org.json.JSONObject;

public class Server {
    private String mDevice = null;

    private Version mVersion = null;

    private String mError = null;

    protected Server() {
    }

    protected String getDevice() {
        return mDevice;
    }

    protected Version getVersion() {
        return mVersion;
    }

    public String getUrl(final String device, final Version version) {
        mDevice = device;
        mVersion = version;
        return null;
    }

    public UpdatePackage[] createPackageList(final JSONObject response) {
        return new UpdatePackage[0];
    }

    public String getError() {
        return mError;
    }

    protected void setError(final String error) {
        mError = error;
    }
}
