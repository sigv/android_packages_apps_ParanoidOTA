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

import android.os.AsyncTask;

import com.paranoid.paranoidota.NetStringReader.NetStringResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/** Network data reader parser. */
public class NetStringReader extends AsyncTask<String, Void, NetStringResult> {

    /** Network data event listener. */
    public static interface NetStringListener {

        /**
         * Handles an unsuccessful network data receipt.
         * 
         * @param e the exception due to which the request failed
         */
        public void onReadError(final Exception e);

        /**
         * Handles a successful network data receipt.
         * 
         * @param buffer the data received for the request
         */
        public void onReadFinish(final String buffer);

    }

    /** @hide */
    public static class NetStringResult {

        /** The network data object. */
        private final String mBuffer;

        /** The network failure object. */
        private final Exception mException;

        /**
         * Initializes the result object.
         * 
         * @param exception the exception which caused the request to fail
         */
        private NetStringResult(Exception exception) {
            mException = exception;
            mBuffer = null;
        }

        /**
         * Initializes the result object.
         * 
         * @param buffer the data received in the request
         */
        private NetStringResult(String buffer) {
            mBuffer = buffer;
            mException = null;
        }

    }

    /** The objects listening to this task. */
    private final NetStringListener[] mListeners;

    /**
     * Initializes the task object.
     * 
     * @param listeners the objects which should be notified about completion
     */
    public NetStringReader(final NetStringListener... listeners) {
        mListeners = listeners;
    }

    @Override
    protected NetStringResult doInBackground(final String... params) {
        final StringBuffer string = new StringBuffer();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new URL(params[0])
                    .openConnection().getInputStream()));
        } catch (IOException e) {
            return new NetStringResult(e);
        }

        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                string.append(line).append("\n");
            }
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException ignore) {
                // we are already returning an exception...
            }

            return new NetStringResult(e);
        }

        try {
            in.close();
        } catch (IOException e) {
            // we don't care about this here as we already have the output
        }

        return new NetStringResult(string.toString());
    }

    @Override
    protected void onPostExecute(NetStringResult result) {
        super.onPostExecute(result);

        for (NetStringListener listener : mListeners) {
            if (listener != null) {
                if (result.mException != null) {
                    listener.onReadError(result.mException);
                } else if (result.mBuffer != null) {
                    listener.onReadFinish(result.mBuffer);
                }
            }
        }
    }

}
