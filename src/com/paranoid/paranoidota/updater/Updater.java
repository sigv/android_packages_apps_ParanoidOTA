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

import android.app.Activity;
import android.content.Context;
import android.net.SSLCertificateSocketFactory;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.paranoid.paranoidota.Utils;
import com.paranoid.paranoidota.Version;
import com.paranoid.paranoidota.helpers.SettingsHelper;
import com.paranoid.paranoidota.updater.server.Server;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Updater implements Response.Listener<JSONObject>, Response.ErrorListener {

    public static interface UpdaterListener {
        public void onCheckStart(final Updater source);

        public void onCheckFinish(final Updater source, final UpdatePackage[] info);

        public void onCheckError(final Updater source, final String reason);
    }

    /** The context to use during the update process. */
    private final Context mContext;

    /** Whether the check was initiated automatically, from an alarm. */
    private final boolean mFromAlarm;

    /** The text to display upon failure. */
    private final String mErrorString;

    /** The servers to check. */
    private final Server[] mServers;

    /** The queue to place networking requests inside of. */
    private final RequestQueue mQueue;

    /** The helpful settings helper. */
    private final SettingsHelper mSettingsHelper;

    private UpdatePackage[] mLastUpdates = new UpdatePackage[0];
    private List<UpdaterListener> mListeners = new ArrayList<UpdaterListener>();
    private boolean mScanning = false;
    private boolean mServerWorks = false;
    private int mCurrentServerIndex = -1;
    private Server mCurrentServer = null;

    public Updater(final Context context, final boolean fromAlarm, final int errorStringResId,
            final Server...servers) {
        mContext = context;
        mFromAlarm = fromAlarm;
        mErrorString = context.getResources().getString(errorStringResId);
        mServers = servers;
        mQueue = Volley.newRequestQueue(context, new HurlStack(null,
                SSLCertificateSocketFactory.getDefault(0, null)));
        mSettingsHelper = new SettingsHelper(getContext());
    }

    public abstract String getSystemCardText(final Context context);

    public abstract String getUrl(final Server currentServer);

    protected Context getContext() {
        return mContext;
    }

    protected SettingsHelper getSettingsHelper() {
        return mSettingsHelper;
    }

    public UpdatePackage[] getLastUpdates() {
        return mLastUpdates;
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void setLastUpdates(final UpdatePackage[] infos) {
        if (infos == null) {
            mLastUpdates = new UpdatePackage[0];
        } else {
            mLastUpdates = infos;
        }
    }

    public Updater addListener(final UpdaterListener listener) {
        if (listener != null) {
            mListeners.add(listener);
        }
        return this;
    }

    public Updater removeListener(final UpdaterListener listener) {
        mListeners.remove(listener);
        return this;
    }

    /**
     * Starts the update checking process.
     */
    public void check() {
        check(false);
    }

    /**
     * Starts the update checking process.
     *
     * @param forceAlarm whether the check should happen even if the alarm timing says otherwise
     */
    public synchronized void check(final boolean forceAlarm) {
        if (mScanning || (mFromAlarm && !forceAlarm && mSettingsHelper.getCheckTime() < 0)) {
            return;
        }

        mScanning = true;
        mServerWorks = false;
        mCurrentServerIndex = -1;
        mCurrentServer = null;

        final Context c = getContext();
        if (c instanceof Activity) {
            final Activity a = (Activity) c;
            final Updater src = this;
            for (final UpdaterListener listener : mListeners) {
                a.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        listener.onCheckStart(src);
                    }

                });
            }
        }

        checkNextServer();
    }

    /**
     * Starts checking the next available server. If there are no more available servers, this
     * call is a no-op.
     *
     * @return {@code true} if a new check was started
     */
    protected synchronized boolean checkNextServer() {
        if (!mScanning) {
            // just abort
            return false;
        }

        if (++mCurrentServerIndex >= mServers.length) {
            // no more servers in the array
            mScanning = false;
            return false;
        }

        mCurrentServer = mServers[mCurrentServerIndex];
        mQueue.add(new JsonObjectRequest(Request.Method.GET, getUrl(mCurrentServer),
                null, this, this));
        return true;
    }

    @Override
    public void onResponse(JSONObject response) {
        setLastUpdates(null);

        final UpdatePackage[] lastUpdates = stripBadGapps(
                mCurrentServer.createPackageList(response));

        if (lastUpdates.length > 0) {
            mServerWorks = true;
            if (mFromAlarm) {
                Utils.showNotification(getContext(), lastUpdates);
            }
        } else {
            final String error = mCurrentServer.getError(); // has to be run after list creation
            if (error == null || "".equals(error)) {
                mServerWorks = true;
                if (checkNextServer()) {
                    return;
                }
            } else if (doHandleError(error)) {
                return;
            }
        }

        mScanning = false;
        setLastUpdates(lastUpdates);

        final Context c = getContext();
        if (c instanceof Activity) {
            final Activity a = (Activity) c;
            final Updater src = this;
            for (final UpdaterListener listener : mListeners) {
                a.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        listener.onCheckFinish(src, lastUpdates);
                    }

                });
            }
        }
    }

    private UpdatePackage[] stripBadGapps(final UpdatePackage[] originals) {
        if (originals == null) {
            return new UpdatePackage[0];
        }

        if (originals.length == 0) {
            return new UpdatePackage[0];
        }

        final ArrayList<UpdatePackage> list = new ArrayList<UpdatePackage>();
        final int gappsType = mSettingsHelper.getGappsType();

        for (final UpdatePackage pack : originals) {
            if (pack.isGapps()) {
                final String filename = pack.getFilename();
                switch (gappsType) {
                case SettingsHelper.GAPPS_MINI:
                    if (filename.contains("-mini")) {
                        list.add(pack);
                    }
                    break;
                case SettingsHelper.GAPPS_STOCK:
                    if (filename.contains("-stock")) {
                        list.add(pack);
                    }
                    break;
                case SettingsHelper.GAPPS_FULL:
                    if (filename.contains("-full")) {
                        list.add(pack);
                    }
                    break;
                case SettingsHelper.GAPPS_MICRO:
                    if (filename.contains("-micro")) {
                        list.add(pack);
                    }
                    break;
                }
            } else {
                list.add(pack);
            }
        }

        return list.toArray(new UpdatePackage[list.size()]);
    }

    @Override
    public void onErrorResponse(final VolleyError ex) {
        doHandleError(null);
    }

    private boolean doHandleError(final String error) {
        if (checkNextServer()) {
            return true;
        }

        if (!mFromAlarm && !mServerWorks) {
            final Context c = getContext();
            final String text = mErrorString + (error == null ? "" : ": " + error);

            if (c instanceof Activity) {
                Utils.showToast((Activity) c, text);
            } else {
                throw new RuntimeException(text);
            }
        }

        mScanning = false;

        final Context c = getContext();
        if (c instanceof Activity) {
            final Activity a = (Activity) c;
            final Updater src = this;
            for (final UpdaterListener listener : mListeners) {
                a.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        listener.onCheckFinish(src, getLastUpdates());
                        listener.onCheckError(src, error);
                    }

                });
            }
        }

        return false;
    }
}
