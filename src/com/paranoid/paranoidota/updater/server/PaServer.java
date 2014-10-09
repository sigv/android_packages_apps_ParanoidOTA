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

import com.paranoid.paranoidota.IOUtils;
import com.paranoid.paranoidota.Version;
import com.paranoid.paranoidota.updater.UpdatePackage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PaServer extends Server {

    public PaServer() {
        super();
    }

    @Override
    public String getUrl(final String device, final Version version) {
        super.getUrl(device, version);
        return String.format("http://api.paranoidandroid.co/updates/%s", device);
    }

    @Override
    public UpdatePackage[] createPackageList(final JSONObject response) {
        super.createPackageList(response);

        final String error = response.optString("error");
        if (error != null && !"".equals(error)) {
            // error out
            setError(error);
            return new UpdatePackage[0];
        }

        setError(null);

        final JSONArray updates = response.optJSONArray("updates");
        if (updates == null) {
            // got nothing - return nothing
            // TODO provide an error message maybe?
            return new UpdatePackage[0];
        }

        final ArrayList<UpdatePackage> list = new ArrayList<UpdatePackage>();
        for (int i = 0; i < updates.length(); i++) {
            final JSONObject file = updates.optJSONObject(i);
            if (file == null) {
                continue; // whatever; just carry on
            }

            final String filename = file.optString("name");
            final String[] bits = filename.replace(".zip", "").split("-");
            if (!bits[bits.length - 1].matches("[-+]?\\d*\\.?\\d+")) {
                continue; // don't add it; just carry on
            }

            final Version version = Version.parseSafePA(filename);
            final Version currentVersion = getVersion();
            if (version.isNewerThanOrEqualTo(currentVersion)) {
                try {
                    list.add(new UpdatePackage(getDevice(), version, filename,
                            IOUtils.parseHumanReadableByteCount(file.optString("size")),
                            file.optString("md5"), new URL(file.optString("url"))));
                } catch (final MalformedURLException e) {
                    // take the ship down
                    throw new RuntimeException("Unable to construct a download link", e);
                }
            }
        }

        // TODO investigate what the f**k is up with the reverse (like seriously)
        Collections.sort(list);
        Collections.reverse(list);

        return list.toArray(new UpdatePackage[list.size()]);
    }
}
