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

import android.content.Context;

import com.paranoid.paranoidota.Utils;
import com.paranoid.paranoidota.Version;
import com.paranoid.paranoidota.helpers.SettingsHelper;
import com.paranoid.paranoidota.signalv.R;
import com.paranoid.paranoidota.updater.server.GooServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class GappsUpdater extends Updater {

    private final Version mRomVersion;

    private final String mPlatform;

    private final String mType;

    private final String mVersion;

    public GappsUpdater(final Context context, final boolean fromAlarm) {
        super(context, fromAlarm, new Server[] { new GooServer(context, false) },
                R.string.check_gapps_updates_error);

        mRomVersion = Version.parseSafePA(RomUpdater.getVersionString(context));

        String platform = Utils.getProp("ro.build.version.release").replace(".", "");
        while (platform.length() < 3) {
            platform += "0";
        }
        mPlatform = platform;

        final Properties props = new Properties();

        try {
            final FileInputStream in = new FileInputStream(new File("/system/etc/g.prop"));
            props.load(in);
            in.close();
        } catch (final FileNotFoundException e) {
            // we carry on with empty properties; we got either missing or non-PA GApps
        } catch (final IOException e) {
            // we carry on with empty properties; we got a fucked up configuration
        }

        String type = props.getProperty("ro.addon.pa_type");
        mType = type == null ? "" : type;

        String version = "0";
        String fullVersion = props.getProperty("ro.addon.pa_version");
        if (fullVersion == null || "".equals(fullVersion)) {
            fullVersion = props.getProperty("ro.addon.version");
        }
        if (fullVersion != null && !"".equals(fullVersion)) {
            final String[] versions = fullVersion.split("-");
            for (final String v : versions) {
                try {
                    Integer.parseInt(new String(new char[] { v.charAt(0) }));
                    version = v;
                    break;
                } catch (final NumberFormatException e) {
                    // ignore and continue looking
                }
            }
        }
        mVersion = version;
    }

    @Override
    public GappsUpdater addListener(final UpdaterListener listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public GappsUpdater removeListener(final UpdaterListener listener) {
        super.addListener(listener);
        return this;
    }

    private Version getVersion() {
        return Version.parseSafePA("gapps-" +
                (mPlatform.length() > 0 ? mPlatform.substring(0, 1) : "0") + "." +
                (mPlatform.length() > 1 ? mPlatform.substring(1) : "0") + "-" +
                mVersion);
    }

    private int getSettingsType() {
        if ("micro".equals(mType)) {
            return SettingsHelper.GAPPS_MICRO;
        } else if ("mini".equals(mType)) {
            return  SettingsHelper.GAPPS_MINI;
        } else if ("stock".equals(mType)) {
            return SettingsHelper.GAPPS_STOCK;
        } else {
            return SettingsHelper.GAPPS_FULL;
        }
    }

    @Override
    public String getSystemCardText(final Context context) {
        return context.getResources().getString(R.string.system_gapps, mType,
                getVersion().toDisplayString());
    }

    @Override
    public String getUrl(final Server currentServer) {
        final Context c = getContext();

        final String device;
        final String root = "GApps/Android%20" + mRomVersion.getMajorVersion() + "." +
                mRomVersion.getMinorVersion();
        switch (getSettingsHelper().getGappsType(getSettingsType())) {
        case SettingsHelper.GAPPS_MICRO:
            device = root + "/Micro-Modular%20GApps";
            break;
        case SettingsHelper.GAPPS_MINI:
            device = root + "/Mini-Modular%20GApps";
            break;
        case SettingsHelper.GAPPS_STOCK:
            device = root + "/Google%20Stock%20GApps";
            break;
        case SettingsHelper.GAPPS_FULL:
        default:
            device = root + "/Full-Modular%20GApps";
            break;
        }

        return currentServer.getUrl(device, getVersion());
    }
}
