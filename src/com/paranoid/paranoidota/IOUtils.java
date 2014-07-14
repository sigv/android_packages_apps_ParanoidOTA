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

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

/** Generic convenience class containing I/O helper tools. */
public class IOUtils {

    /** Logging tag for the class. */
    private static final String TAG = "IOUtils";

    private static Properties sDictionary;

    /** The mount point of the primary SD card or a null value if none exist. */
    private static String sMountPointPrimarySdcard = null;

    /** The mount point of the secondary SD card or a null value if none exist. */
    private static String sMountPointSecondarySdcard = null;

    /** Boolean value informing whether the mount points have to be checked. */
    private static boolean sShouldCheckMounts = true;

    static {
        checkMounts();
    }

    /** @return {@code true} if the primary external storage is present & mounted */
    public static boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /** @return the mount point of the primary SD card */
    public static String getPrimarySdCard() {
        if (sMountPointPrimarySdcard == null) {
            if (sShouldCheckMounts) {
                checkMounts();
            } else {
                sMountPointPrimarySdcard = Environment.getExternalStorageDirectory()
                        .getAbsolutePath();
            }
        }

        return sMountPointPrimarySdcard;
    }

    /**
     * @return the mount point of the secondary SD card or a null value if no
     *         such mount point has been located
     */
    public static String getSecondarySdCard() {
        return sMountPointSecondarySdcard;
    }

    /** Checks for the primary and secondary external storages. */
    private synchronized static void checkMounts() {
        if (!sShouldCheckMounts) {
            return;
        }

        sShouldCheckMounts = false;
        sMountPointPrimarySdcard = null;
        sMountPointSecondarySdcard = null;

        ArrayList<String> mounts = new ArrayList<String>();
        ArrayList<String> vold = new ArrayList<String>();

        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("/proc/mounts"));
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("/dev/block/vold/")) {
                    String[] lineElements = line.split(" ");
                    String element = lineElements[1];

                    mounts.add(element);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        boolean addExternal = mounts.size() == 1 && isExternalStorageMounted();
        if (mounts.size() == 0 && addExternal) {
            mounts.add("/mnt/sdcard");
        }
        File fstab = findFstab();
        scanner = null;
        if (fstab != null) {
            try {

                scanner = new Scanner(fstab);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("dev_mount")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[2];

                        if (element.contains(":")) {
                            element = element.substring(0, element.indexOf(":"));
                        }

                        if (element.toLowerCase().indexOf("usb") < 0) {
                            vold.add(element);
                        }
                    } else if (line.startsWith("/devices/platform")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[1];

                        if (element.contains(":")) {
                            element = element.substring(0, element.indexOf(":"));
                        }

                        if (element.toLowerCase().indexOf("usb") < 0) {
                            vold.add(element);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
        if (addExternal && (vold.size() == 1 && isExternalStorageMounted())) {
            mounts.add(vold.get(0));
        }
        if (vold.size() == 0 && isExternalStorageMounted()) {
            vold.add("/mnt/sdcard");
        }

        for (int i = 0; i < mounts.size(); i++) {
            String mount = mounts.get(i);
            File root = new File(mount);
            if (!vold.contains(mount)
                    || (!root.exists() || !root.isDirectory() || !root.canWrite())) {
                mounts.remove(i--);
            }
        }

        for (int i = 0; i < mounts.size(); i++) {
            String mount = mounts.get(i);
            if (mount.indexOf("sdcard0") < 0 && !mount.equalsIgnoreCase("/mnt/sdcard")
                    && !mount.equalsIgnoreCase("/sdcard")) {
                sMountPointSecondarySdcard = mount;
            } else {
                sMountPointPrimarySdcard = mount;
            }
        }

        if (sMountPointPrimarySdcard == null) {
            sMountPointPrimarySdcard = "/sdcard";
        }
    }

    /** @return the current fstab file or a null value if none can be found */
    private static File findFstab() {
        File vold = new File("/system/etc/vold.fstab");
        if (vold.exists()) {
            return vold;
        }

        for (String fstab : Utils.exec("grep -ls \"/dev/block/\" /fstab.*").split("\n")) {
            File file = new File(fstab);
            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    /** @return count of gigabytes of space left on the primary external storage */
    public static double getGbLeftOnPrimarySdCard() {
        final StatFs stat = new StatFs(getPrimarySdCard());
        return ((double) stat.getAvailableBlocksLong() * (double) stat.getBlockSizeLong())
                / (1024 * 1024 * 1024);
    }

    /**
     * @param bytes count of bytes
     * @param si {@code true} if the blocks should be exactly 1000 units large;
     *            {@code false} if 1024
     * @return formatted size value readable by normal human beings
     */
    public static String formatHumanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;

        if (bytes < unit) {
            return bytes + " B";
        }

        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp),
                (si ? "kMG" : "KMG").charAt(exp - 1) + (si ? "" : "i")).replace(",", ".");
    }

    /**
     * @param file the source file
     * @return the generated md5 checksum
     * @throws FileNotFoundException in the case of the requested file not
     *             existing and being available for checking
     */
    public static String md5(File file) throws FileNotFoundException {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is not a valid and available algorithm.", e);
        }

        final InputStream is = new FileInputStream(file);

        byte[] buffer = new byte[8192];
        int read = 0;

        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            // in the case of a reading failure, we just roll with what we got
        }

        try {
            is.close();
        } catch (IOException e) {
            // we tried - we failed - move on
        }

        String md5 = new BigInteger(1, digest.digest()).toString(16);
        while (md5.length() < 32) {
            md5 = "0" + md5;
        }
        return md5;
    }

    /**
     * Loads the local dictionary. This call might be a blocking call if the
     * dictionary has not yet been loaded into the memory.
     * 
     * @param context context to use for loading which could be needed
     * @return the local dictionary
     * @throws RuntimeException in case an attempt to load the dictionary from
     *             the storage was done but failed
     */
    public static Properties getDictionary(Context context) {
        synchronized (IOUtils.class) {
            if (sDictionary == null) {
                sDictionary = new Properties();
                try {
                    sDictionary.load(context.getAssets().open("dictionary.properties"));
                } catch (IOException e) {
                    // we are suppressing this as the application can carry on
                    // with the empty properties object which was just created
                    Log.w(TAG, "The local dictionary could not be loaded.", e);
                }
            }
        }

        return sDictionary;
    }

    /**
     * @return the downloads directory object for use right away
     * @throws RuntimeException in the case that the directory creation fails
     *             for any reason and there is no preferred directory to use
     */
    public static File getDownloadsDirectory() {
        final File dir = new File(Environment.getExternalStorageDirectory(), "paranoidota/");
        dir.mkdirs();

        if (!dir.isDirectory()) {
            throw new RuntimeException("The download directory is not initialized.");
        }

        return dir;
    }

    /**
     * @return {@code true} if the <i>.android-secure</i> directory exists on
     *         the primary external storage
     */
    public static boolean hasAndroidSecure() {
        final File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/.android-secure");
        return f.exists() && f.isDirectory();
    }

    /**
     * @return {@code true} if the <i>sd-ext</i> directory exists on the root of
     *         the file system
     */
    public static boolean hasSdExt() {
        final File f = new File("/sd-ext");
        return f.exists() && f.isDirectory();
    }

    /** @hide */
    private IOUtils() {
    }

}
