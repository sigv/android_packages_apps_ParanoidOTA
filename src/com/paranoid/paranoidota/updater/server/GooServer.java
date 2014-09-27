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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GooServer implements Server {

    private static final String URL = "https://api.goo.im/files/devs/paranoidandroid/roms/%s?ro_board=%s";
    private static final String GAPPS_RESERVED_WORDS = "-signed|-modular|-full|-mini|-micro|-stock";

    private Context mContext;
    private String mDevice = null;
    private String mError = null;
    private Version mVersion;
    private boolean mIsRom;

    public GooServer(Context context, boolean isRom) {
        mContext = context;
        mIsRom = isRom;
    }

    @Override
    public String getUrl(String device, Version version) {
        mDevice = device;
        mVersion = version;
        return String.format(URL, new Object[] {
                device, device
        });
    }

    @Override
    public List<UpdatePackage> createPackageList(JSONObject response) throws Exception {
        List<UpdatePackage> list = new ArrayList<UpdatePackage>();
        mError = null;
        JSONArray updates = response.optJSONArray("files");
        if (updates == null) {
            mError = mContext.getResources().getString(R.string.error_device_not_found_server);
        }
        for (int i = 0; updates != null && i < updates.length(); i++) {
            JSONObject file = updates.getJSONObject(i);
            String onlinePath = file.optString("path");
            if (onlinePath != null && !onlinePath.isEmpty() && onlinePath.endsWith(".zip")) {
                String[] pathParts = onlinePath.split("/");
                String filename = pathParts[pathParts.length - 1];
                String stripped = filename.replace(".zip", "");
                if (!mIsRom) {
                    stripped = stripped.replaceAll("\\b(" + GAPPS_RESERVED_WORDS + ")\\b", "");
                }
                String[] parts = stripped.split("-");
                boolean isNew = parts[parts.length - 1].matches("[-+]?\\d*\\.?\\d+");
                if (!isNew) {
                    if (!mIsRom) {
                        String part = parts[parts.length - 1];
                        isNew = Utils.isDouble(part)
                                || Utils.isDouble(part.substring(0,
                                        part.length() - 1));
                        if (!isNew) {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
                Version version = Version.parseSafePA(filename);
                if (version.isNewerThanOrEqualTo(mVersion)) {
                    list.add(new UpdatePackage(mIsRom ? mDevice : UpdatePackage.DEVICE_NAME_GAPPS,
                            version, filename, file.getLong("filesize"), file.getString("md5"),
                            new URL("https://goo.im" + file.getString("path"))));
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
