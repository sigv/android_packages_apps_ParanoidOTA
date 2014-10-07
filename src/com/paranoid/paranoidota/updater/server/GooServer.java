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

import android.content.Context;

import com.paranoid.paranoidota.Utils;
import com.paranoid.paranoidota.Version;
import com.paranoid.paranoidota.signalv.R;
import com.paranoid.paranoidota.updater.Server;
import com.paranoid.paranoidota.updater.UpdatePackage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GooServer implements Server {

    private static final String GAPPS_RESERVED_WORDS = "-signed|-modular|-full|-mini|-micro|-stock";

    private final Context mContext;

    private String mDevice = null;

    private Version mVersion = null;

    private String mError = null;

    public GooServer(final Context context) {
        mContext = context;
    }

    @Override
    public String getUrl(final String device, final Version version) {
        mDevice = device;
        mVersion = version;

        return String.format("https://api.goo.im/files/devs/paranoidandroid/roms/%s?ro_board=%s",
                device, device);
    }

    @Override
    public UpdatePackage[] createPackageList(JSONObject response) {
        final JSONArray files = response.optJSONArray("files");
        if (files == null) {
            // got nothing - return nothing
            mError = mContext.getResources().getString(R.string.error_device_not_found_server);
            return new UpdatePackage[0];
        }

        final ArrayList<UpdatePackage> list = new ArrayList<UpdatePackage>();
        for (int i = 0; i < files.length(); i++) {
            final JSONObject file = files.optJSONObject(i);
            if (file == null) {
                continue; // whatever; just carry on
            }

            final String path = file.optString("path");
            if (path == null) {
                continue; // whatever; just carry on
            }
            if (path.isEmpty() || !path.endsWith(".zip")) {
                continue; // whatever; just carry on
            }

            final String[] bits = path.split("/");
            final String filename = bits[bits.length - 1];

            final String[] strippedBits = filename.replace(".zip", "")
                    .replaceAll("\\b(" + GAPPS_RESERVED_WORDS + ")\\b", "").split("-");
            final String lastStrippedBit = strippedBits[strippedBits.length - 1];
            if (!lastStrippedBit.matches("[-+]?\\d*\\.?\\d+") &&
                    !Utils.isDouble(lastStrippedBit) &&
                    !Utils.isDouble(lastStrippedBit.substring(0, lastStrippedBit.length() - 1))) {
                continue; // don't add it; just carry on
            }

            final Version version = Version.parseSafePA(filename);
            if (version.isNewerThanOrEqualTo(mVersion)) {
                URL url = null;
                try {
                    url = new URL("https://goo.im" + path);
                } catch (final MalformedURLException e) {
                    // abandon ship
                    try {
                        url = new URL("https://goo.im/");
                    } catch (final MalformedURLException fuuuu) {
                        // take the ship down with you
                        final RuntimeException throwable = new RuntimeException(
                                "Unable to construct a download link", fuuuu);
                        throwable.addSuppressed(e);
                        throw throwable;
                    }
                }

                list.add(new UpdatePackage(filename.contains("pa_gapps") ?
                        UpdatePackage.DEVICE_NAME_GAPPS : mDevice, version, filename,
                        file.optLong("filesize", 0), file.optString("md5"), url));
            }
        }

        // TODO investigate what the f**k is up with the reverse (like seriously)
        Collections.sort(list);
        Collections.reverse(list);

        return list.toArray(new UpdatePackage[list.size()]);
    }

    @Override
    public String getError() {
        return mError;
    }

}
