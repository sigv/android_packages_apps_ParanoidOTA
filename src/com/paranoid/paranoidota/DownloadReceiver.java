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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.paranoid.paranoidota.activities.MainActivity;

/** Download completion and notification listener. */
public class DownloadReceiver extends BroadcastReceiver {

    /** {@inheritDoc} */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        context.startActivity(new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Utils.Extras.CHECK_DOWNLOADS_FINISHED.getName(), true)
                .putExtra(Utils.Extras.CHECK_DOWNLOADS_ID.getName(),
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object another) {
        // All DownloadReceiver objects are identical.
        return another instanceof DownloadReceiver;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getName();
    }

}
