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
import com.paranoid.paranoidota.signalv.R;
import com.paranoid.paranoidota.updater.server.GooServer;
import com.paranoid.paranoidota.updater.server.PaServer;

public class RomUpdater extends Updater {

    private static String getDevice(final Context context) {
        String device = Utils.getProp("ro.pa.device");
        if (device == null || "".equals(device)) {
            device = Utils.translateDeviceName(context, Utils.getProp("ro.product.device"));
        }
        return device == null ? "" : device.toLowerCase();
    }

    public static String getVersionString(final Context context) {
        return getDevice(context) + "-" + Utils.getProp("ro.modversion");
    }

    public RomUpdater(final Context context, final boolean fromAlarm) {
        super(context, fromAlarm, new Server[] { new PaServer(), new GooServer(context) },
                R.string.check_rom_updates_error);
    }

    @Override
    public RomUpdater addListener(final UpdaterListener listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public RomUpdater removeListener(final UpdaterListener listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public String getSystemCardText(final Context context) {
        return context.getResources().getString(R.string.system_rom,
                Version.parseSafePA(getVersionString(context)).toDisplayString());
    }

    @Override
    public String getUrl(final Server currentServer) {
        final Context c = getContext();
        return currentServer.getUrl(getDevice(c), Version.parseSafePA(getVersionString(c)));
    }
}
