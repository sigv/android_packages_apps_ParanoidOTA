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
import com.paranoid.paranoidota.updater.Server;
import com.paranoid.paranoidota.updater.UpdatePackage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PaServer implements Server {

    private static final String URL = "http://api.paranoidandroid.co/updates/%s";

    private String mDevice = null;
    private String mError = null;
    private Version mVersion;

    @Override
    public String getUrl(String device, Version version) {
        mDevice = device;
        mVersion = version;
        return String.format(URL, new Object[] {
                device
        });
    }

    @Override
    public List<UpdatePackage> createPackageList(JSONObject response) throws Exception {
        mError = null;
        List<UpdatePackage> list = new ArrayList<UpdatePackage>();
        mError = response.optString("error");
        if (mError == null || mError.isEmpty()) {
            JSONArray updates = response.getJSONArray("updates");
            for (int i = updates.length() - 1; i >= 0; i--) {
                JSONObject file = updates.getJSONObject(i);
                String filename = file.optString("name");
                String stripped = filename.replace(".zip", "");
                String[] parts = stripped.split("-");
                boolean isNew = parts[parts.length - 1].matches("[-+]?\\d*\\.?\\d+");
                if (!isNew) {
                    continue;
                }
                Version version = Version.parseSafePA(filename);
                if (version.isNewerThanOrEqualTo(mVersion)) {
                    list.add(new UpdatePackage(mDevice, version, filename,
                            Long.parseLong(file.getString("size")), file.getString("md5"),
                            new URL(file.getString("url"))));
                }
            }
        }
        Collections.sort(list, new Comparator<UpdatePackage>() {

            @Override
            public int compare(UpdatePackage lhs, UpdatePackage rhs) {
                return lhs.getVersion().compareTo(rhs.getVersion());
            }

        });
        Collections.reverse(list);
        return list;
    }

    @Override
    public String getError() {
        return mError;
    }

}
