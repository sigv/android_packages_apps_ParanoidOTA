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
import android.app.Service;
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
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

public class Utils {

    public static final String FILES_INFO = "com.paranoid.paranoidota.Utils.FILES_INFO";
    public static final String CHECK_DOWNLOADS_FINISHED = "com.paranoid.paranoidota.Utils.CHECK_DOWNLOADS_FINISHED";
    public static final String CHECK_DOWNLOADS_ID = "com.paranoid.paranoidota.Utils.CHECK_DOWNLOADS_ID";
    public static final String MOD_VERSION = "ro.modversion";
    public static final String RO_PA_VERSION = "ro.pa.version";

    /** The different alarm types. */
    public static enum AlarmType {
        /** The alarm for the ROM. */
        ROM(122303221),
        /** The alarm for the Google apps. */
        GAPPS(122303222);

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

    /** The different recovery types. */
    public static enum RecoveryType {
        /** The recovery type for TWRP. */
        TWRP(1),
        /** The recovery type for all CWM-based recoveries. */
        CWM_BASED(2);

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

    public static PackageInfo[] sPackageInfosRom = new PackageInfo[0];
    public static PackageInfo[] sPackageInfosGapps = new PackageInfo[0];
    private static Typeface sRobotoThin;

    /**
     * The information to be passed on between the components of the application
     * as a part of a notification.
     */
    public static class NotificationInfo implements Serializable {
        private static final long serialVersionUID = -9013072595529661176L;

        /** The ID of the current notification in question. */
        public final int mNotificationId;

        /** Information about the ROM packages in the latest update. */
        public final PackageInfo[] mRomPackages;

        /** Information about the GApps packages gathered in the latest update. */
        public final PackageInfo[] mGappsPackages;

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

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void setAlarm(Context context, boolean trigger, boolean isRom) {

        SettingsHelper helper = new SettingsHelper(context);
        setAlarm(context, helper.getCheckTime(), trigger, isRom);
    }

    public static void setAlarm(Context context, long time, boolean trigger, boolean isRom) {

        Intent i = new Intent(context, NotificationAlarm.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getBroadcast(context,
                isRom ? AlarmType.ROM.mId : AlarmType.GAPPS.mId, i,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        if (time > 0) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, trigger ? 0 : time, time, pi);
        }
    }

    public static boolean alarmExists(Context context, boolean isRom) {
        return (PendingIntent.getBroadcast(context, isRom ? AlarmType.ROM.mId
                : AlarmType.GAPPS.mId, new Intent(context, NotificationAlarm.class),
                PendingIntent.FLAG_NO_CREATE) != null);
    }

    public static void showToastOnUiThread(final Context context, final int resourceId) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, resourceId, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showToastOnUiThread(final Context context, final String string) {
        ((Activity) context).runOnUiThread(new Runnable() {

            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void showNotification(Context context, Updater.PackageInfo[] infosRom,
            Updater.PackageInfo[] infosGapps) {
        Resources resources = context.getResources();

        if (infosRom != null) {
            sPackageInfosRom = infosRom;
        } else {
            infosRom = sPackageInfosRom;
        }
        if (infosGapps != null) {
            sPackageInfosGapps = infosGapps;
        } else {
            infosGapps = sPackageInfosGapps;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(FILES_INFO, new NotificationInfo(Updater.NOTIFICATION_ID, infosRom,
                infosGapps));
        PendingIntent pIntent = PendingIntent.getActivity(context, Updater.NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(resources.getString(R.string.new_system_update))
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher))
                .setContentIntent(pIntent);

        String contextText = "";
        if (infosRom.length + infosGapps.length == 1) {
            String filename = infosRom.length == 1 ? infosRom[0].getFilename() : infosGapps[0]
                    .getFilename();
            contextText = resources.getString(R.string.new_package_name, new Object[] {
                    filename
            });
        } else {
            contextText = resources.getString(R.string.new_packages, new Object[] {
                    infosRom.length
                            + infosGapps.length
            });
        }
        builder.setContentText(contextText);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(context.getResources().getString(R.string.new_system_update));
        if (infosRom.length + infosGapps.length > 1) {
            inboxStyle.addLine(contextText);
        }
        for (int i = 0; i < infosRom.length; i++) {
            inboxStyle.addLine(infosRom[i].getFilename());
        }
        for (int i = 0; i < infosGapps.length; i++) {
            inboxStyle.addLine(infosGapps[i].getFilename());
        }
        inboxStyle.setSummaryText(resources.getString(R.string.app_name));
        builder.setStyle(inboxStyle);

        Notification notif = builder.build();

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Service.NOTIFICATION_SERVICE);

        notif.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(Updater.NOTIFICATION_ID, notif);
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ex) {
        }
        return false;
    }

    public static String exec(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("sync\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            return getStreamLines(p.getInputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static String getStreamLines(final InputStream is) {
        String out = null;
        StringBuffer buffer = null;
        final DataInputStream dis = new DataInputStream(is);

        try {
            if (dis.available() > 0) {
                buffer = new StringBuffer(dis.readLine());
                while (dis.available() > 0) {
                    buffer.append("\n").append(dis.readLine());
                }
            }
            dis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (buffer != null) {
            out = buffer.toString();
        }
        return out;
    }

    public static void setRobotoThin(Context context, View view) {
        if (sRobotoThin == null) {
            sRobotoThin = Typeface.createFromAsset(context.getAssets(),
                    "Roboto-Light.ttf");
        }
        setFont(view, sRobotoThin);
    }

    private static void setFont(View view, Typeface robotoTypeFace) {
        if (view instanceof ViewGroup) {
            int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                setFont(((ViewGroup) view).getChildAt(i), robotoTypeFace);
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(robotoTypeFace);
        }
    }
}
