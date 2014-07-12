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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.paranoid.paranoidota.helpers.SettingsHelper;
import com.paranoid.paranoidota.signalv.R;
import com.paranoid.paranoidota.updater.Updater;
import com.paranoid.paranoidota.updater.Updater.PackageInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

public class Utils {

    /** The different alarm types. */
    public static enum AlarmType {
        /** The alarm for the Google apps. */
        GAPPS(122303222),
        /** The alarm for the ROM. */
        ROM(122303221);

        /** The private request code for use with the alarm broadcast. */
        public final int mId;

        /**
         * Initializes an alarm type.
         * 
         * @param id the private request code
         */
        private AlarmType(final int id) {
            mId = id;
        }
    }

    /** The intent extras used by this application. */
    public static enum Extras {
        /** Boolean extra informing if a download should be checked as finished. */
        CHECK_DOWNLOADS_FINISHED(".CHECK_DOWNLOADS_FINISHED"),
        /** Long extra informing which download should be checked. */
        CHECK_DOWNLOADS_ID(".CHECK_DOWNLOADS_ID"),
        /** Notification information extra informing about the package listing. */
        FILES_INFO(".FILES_INFO");

        /** The internal name of the extra. */
        private final String mName;

        /**
         * Initializes the extra.
         * 
         * @param name the internal name of the extra
         */
        private Extras(String name) {
            mName = name;
        }

        /** Gets the name of the extra together with the package name. */
        public String getName() {
            return mName.startsWith(".") ? "com.paranoid.paranoidota" + mName : mName;
        }

    }

    /**
     * The information to be passed on between the components of the application
     * as a part of a notification.
     */
    public static class NotificationInfo implements Serializable {
        private static final long serialVersionUID = -9013072595529661176L;

        /** Information about the GApps packages gathered in the latest update. */
        public final PackageInfo[] mGappsPackages;

        /** The ID of the current notification in question. */
        public final int mNotificationId;

        /** Information about the ROM packages in the latest update. */
        public final PackageInfo[] mRomPackages;

        /**
         * Initializes an information object.
         * 
         * @param notificationId the ID of the displayed notification
         * @param romPackages information about ROM packages
         * @param gappsPackages information about the GApps packages
         */
        public NotificationInfo(int notificationId, PackageInfo[] romPackages,
                PackageInfo[] gappsPackages) {
            mNotificationId = notificationId;
            mRomPackages = romPackages;
            mGappsPackages = gappsPackages;
        }
    }

    /** The different recovery types. */
    public static enum RecoveryType {
        /** The recovery type for all CWM-based recoveries. */
        CWM_BASED(2),
        /** The recovery type for TWRP. */
        TWRP(1);

        /** The internal identifier assigned to the recovery. */
        public final int mId;

        /**
         * Initializes a recovery type.
         * 
         * @param id the internal identifier
         */
        private RecoveryType(final int id) {
            mId = id;
        }
    }

    /** Information about the latest GApps packages. */
    private static PackageInfo[] sGappsPackages = new PackageInfo[0];

    /** The Roboto Thin/Light typeface to be used for themeing. */
    private static Typeface sRobotoThin;

    /** Information about the latest ROM packages. */
    private static PackageInfo[] sRomPackages = new PackageInfo[0];

    /**
     * Checks whether the specified alarm already exists.
     * 
     * @param context the context to use for the check
     * @param alarmType the alarm type which should be looked for
     * @return {@code true} if the alarm does indeed already exist
     */
    public static boolean alarmExists(Context context, AlarmType alarmType) {
        return (PendingIntent.getBroadcast(context, alarmType.mId, new Intent(context,
                NotificationAlarm.class), PendingIntent.FLAG_NO_CREATE) != null);
    }

    /**
     * Change the owner and mode of the specified file. Method inspired by code
     * from OpenDelta (thanks to Jorrit "Chainfire" Jongma and The OmniROM
     * Project).
     * 
     * @param file the path to the file which should be modified
     * @param mode the new mode to apply to the file (e.g. 0777)
     * @param uid the new user ID to apply to the file
     * @param gid the new group ID to apply to the file
     * @throws RuntimeException in the case anything goes horribly wrong
     */
    public static boolean chownmod(String file, int mode, int uid, int gid) {
        if (file == null) {
            throw new IllegalArgumentException("The filename cannot be a null value.");
        }

        try {
            return (Integer) Class.forName("android.os.FileUtils").getMethod("setPermissions",
                    String.class, int.class, int.class, int.class).invoke(null, file,
                    mode, uid, gid) == 0;
        } catch (Exception e) {
            throw new RuntimeException("Unable to change the ownership/mode of " + file,
                    e);
        }
    }

    /**
     * Attempts to execute a system command. This is a magical call that
     * interacts with the runtime and a bunch of things could go wrong here,
     * most of which are going to be wrapped in a {@code RuntimeException}.
     * 
     * @param command the command to execute
     * @return the output of the executed command
     * @throws RuntimeException in the case of anything going horribly wrong
     */
    public static String exec(String command) {
        Process p = null;

        try {
            p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException("The requested command (" + command
                    + ") could not be executed.", e);
        }

        try {
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("sync\n");
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("The data output stream could not be written to.", e);
        }

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("The wait for the requested process was interrupted.", e);
        }

        final InputStream is = p.getInputStream();
        final DataInputStream dis = new DataInputStream(is);

        StringBuffer buffer = new StringBuffer();

        try {
            while (dis.available() > 0) {
                buffer.append("\n").append(dis.readLine());
            }
        } catch (IOException e) {
            throw new RuntimeException("The data input stream could not be read.", e);
        }

        try {
            dis.close();
        } catch (IOException e) {
            throw new RuntimeException("The data input stream could not be closed.", e);
        }

        String output = buffer.toString();
        while (output.startsWith("\n")) {
            output = output.substring(1);
        }
        while (output.endsWith("\n")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    /**
     * Attempts to get the requested system property. It should be expected that
     * if the underlying calls fail for any reason, null is to be returned
     * instead to the caller.
     * 
     * @param prop the name of the property
     * @return the value of the property or null in case of failure
     */
    public static String getProp(String prop) {
        if (prop == null) {
            throw new IllegalArgumentException("The property name cannot be a null value.");
        }

        String out = exec("getprop " + prop);
        if (out == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String s : out.split("\n")) {
            sb.append(s);
        }

        return sb.toString();
    }

    /**
     * Checks whether the string passed in is a number.
     * 
     * @param s the string to evaluate
     * @return {@code true} if the string can be parsed as a {@code double}
     */
    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Checks whether a connected data network is currently available.
     * 
     * @param context the context to use for the check
     * @return {@code true} if there is a network to use
     */
    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Sets an alarm of the specified type.
     * 
     * @param context the context to use for the alarm creation and scheduling
     * @param alarmType the alarm type which the alarm should be created for
     * @param triggerNow whether the alarm should go off now (in 0 milliseconds)
     *            for the first time
     */
    public static void setAlarm(Context context, AlarmType alarmType, boolean triggerNow) {
        setAlarm(context, alarmType, triggerNow, new SettingsHelper(context).getCheckTime());
    }

    /**
     * Sets an alarm of the specified type and interval length.
     * 
     * @param context the context to use for the alarm creation and scheduling
     * @param alarmType the alarm type which the alarm should be created for
     * @param triggerNow whether the alarm should go off now (in 0 milliseconds)
     *            for the first time
     * @param interval the count of milliseconds that should be between each
     *            consecutive firing of the alarm
     */
    public static void setAlarm(Context context, AlarmType alarmType,
            boolean triggerNow, long interval) {
        if (context == null) {
            throw new IllegalArgumentException("The context cannot be a null value.");
        }
        if (alarmType == null) {
            throw new IllegalArgumentException("The alarm type cannot be a null value.");
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("The interval cannot be a non-positive value.");
        }

        PendingIntent intent = PendingIntent.getBroadcast(context,
                alarmType.mId, new Intent(context, NotificationAlarm.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(intent);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerNow ? 0 : interval, interval,
                intent);
    }

    /**
     * Sets the font of the specified view to Roboto Thin/Light.
     * 
     * @param context the context to use for loading the typeface, if needed
     * @param view the {@code ViewGroup} or {@code TextView} to work with
     */
    public static void setRobotoThin(Context context, View view) {
        if (sRobotoThin == null) {
            if (context == null) {
                throw new IllegalArgumentException("The context cannot be a null value.");
            }

            sRobotoThin = Typeface.createFromAsset(context.getAssets(),
                    "Roboto-Light.ttf");
        }

        setTypefaceRecursively(sRobotoThin, view);
    }

    /**
     * Sets the font of the view in a recursive manner.
     * 
     * @param typeface the typeface to use as replacement
     * @param view the {@code ViewGroup} or {@code TextView} to work with
     */
    private static void setTypefaceRecursively(Typeface typeface, View view) {
        if (view instanceof ViewGroup) {
            final int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                setTypefaceRecursively(typeface, ((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }
    }

    /**
     * Displays a notification.
     * 
     * @param context the context to use for the creation of the notification
     * @param romInfo the information about the ROM packages
     * @param gappsInfo the information about the GApps packages
     */
    public static void showNotification(Context context, Updater.PackageInfo[] romInfo,
            Updater.PackageInfo[] gappsInfo) {
        romInfo = romInfo != null ? (sRomPackages = romInfo) : sRomPackages;
        gappsInfo = gappsInfo != null ? (sGappsPackages = gappsInfo) : sGappsPackages;

        final Resources res = context.getResources();

        final PendingIntent intent = PendingIntent.getActivity(context, Updater.NOTIFICATION_ID,
                new Intent(context, MainActivity.class).putExtra(Extras.FILES_INFO.getName(),
                        new NotificationInfo(Updater.NOTIFICATION_ID, romInfo, gappsInfo)),
                PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(res.getString(R.string.new_system_update))
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
                .setContentIntent(intent);

        final String contextText = (romInfo.length + gappsInfo.length == 1) ?
                res.getString(R.string.new_package_name,
                        new Object[] {
                            romInfo.length == 1 ? romInfo[0].getFilename() : gappsInfo[0]
                                    .getFilename()
                        }) :
                res.getString(R.string.new_packages, new Object[] {
                        romInfo.length + gappsInfo.length
                });
        builder.setContentText(contextText);

        final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(context.getResources().getString(R.string.new_system_update));
        if (romInfo.length + gappsInfo.length > 1) {
            inboxStyle.addLine(contextText);
        }
        for (int i = 0; i < romInfo.length; i++) {
            inboxStyle.addLine(romInfo[i].getFilename());
        }
        for (int i = 0; i < gappsInfo.length; i++) {
            inboxStyle.addLine(gappsInfo[i].getFilename());
        }
        inboxStyle.setSummaryText(res.getString(R.string.app_name));
        builder.setStyle(inboxStyle);

        final Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        ((NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE)).notify(Updater.NOTIFICATION_ID,
                notification);
    }

    /**
     * Displays a toast notification.
     * 
     * @param activity the activity to use for displaying the toast
     * @param resourceId the ID of the string resource to be used as the
     *            notification text
     */
    public static void showToast(final Activity activity, final int resourceId) {
        if (activity == null) {
            throw new IllegalArgumentException("The activity cannot be a null value.");
        }

        showToast(activity, activity.getResources().getString(resourceId));
    }

    /**
     * Displays a toast notification.
     * 
     * @param activity the activity to use for displaying the toast
     * @param resourceId the notification text
     */
    public static void showToast(final Activity activity, final String text) {
        if (activity == null) {
            throw new IllegalArgumentException("The activity cannot be a null value.");
        }

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            }

        });
    }

    /**
     * Looks up a translation for the device name.
     * 
     * @param context the current context to be used in lookup
     * @param device the device name which should be translated
     * @return the device name translated as well as possible, being returned as
     *         it was passed in in the worst case scenario
     */
    public static String translateDeviceName(Context context, String device) {
        if (context == null) {
            throw new IllegalArgumentException("The context cannot be a null value.");
        }
        if (device == null) {
            return "null";
        }

        final Properties dictionary = IOUtils.getDictionary(context);

        final String translation = dictionary.getProperty(device);
        if (translation != null) {
            return translation;
        }

        for (String removable : dictionary.getProperty("@remove").split(",")) {
            if (device.contains(removable)) {
                device = device.replace(removable, "");
                break;
            }
        }

        return device;
    }

}
