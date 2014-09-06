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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.paranoid.paranoidota.updater.GappsUpdater;
import com.paranoid.paranoidota.updater.RomUpdater;

/** The alarm receiver handling scheduled update checks. */
public class NotificationAlarmReceiver extends BroadcastReceiver {

    /** The cached GApps updater instance. */
    private GappsUpdater mGappsUpdater;

    /** The cached ROM updater instance. */
    private RomUpdater mRomUpdater;

    /** {@inheritDoc} */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (mRomUpdater == null) {
            mRomUpdater = new RomUpdater(context, true);
        }

        if (mGappsUpdater == null) {
            mGappsUpdater = new GappsUpdater(context, true);
        }

        if (Utils.isNetworkAvailable(context)) {
            mRomUpdater.check();
            mGappsUpdater.check();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object another) {
        // The objects are identical for sure.
        if (this == another) {
            return true;
        }

        // The other object has the wrong type.
        if (!(another instanceof NotificationAlarmReceiver)) {
            return false;
        }

        // Cast and check all the fields.
        final NotificationAlarmReceiver r = (NotificationAlarmReceiver) another;
        return mGappsUpdater.equals(r.mGappsUpdater) && mRomUpdater.equals(r.mRomUpdater);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClass().getName() + "[" + "gappsUpdater=" + mGappsUpdater + ", " +
            "romUpdater=" + mRomUpdater + "]";
    }

}
