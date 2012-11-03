package com.jamie.picturestory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    public static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final String TAG = "Utils";

    private Utils() {};

    /**
     * Workaround for bug pre-Froyo, see here for more info:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     */
    /*public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (hasHttpConnectionBug()) {
            System.setProperty("http.keepAlive", "false");
        }
    }*/

    /**
     * Get the size in bytes of a bitmap.
     * @param bitmap
     * @return size in bytes
     */
    @SuppressLint("NewApi")
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     *         otherwise.
     */
    @SuppressLint("NewApi")
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    /*@SuppressLint("NewApi")
    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }*/

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @SuppressLint("NewApi")
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Get the memory class of this device (approx. per-app memory limit)
     *
     * @param context
     * @return
     */
    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
    
    /** Create a File for saving an image */
    public static Uri getOutputPhotoFileUri() {
    	// TODO: Check that the SDCard is mounted using Environment.getExternalStorageState().

    	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
    				Environment.DIRECTORY_PICTURES), "PictureStory");
    	
    	// Create the storage directory if it does not exist
    	if (!mediaStorageDir.exists()) {
    		if (!mediaStorageDir.mkdirs()) { // Media storage directory made -here-
    			Log.e(TAG, "failed to create directory");
    			return null;
    		}
    	}

    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    	File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

    	return Uri.fromFile(mediaFile);
    }
    
    public static String getOutputAudioFilePath(Context cxt) {
    	String mediaStorageDir = cxt.getExternalFilesDir(null).getAbsolutePath();

    	// Create a media file name
    	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    	mediaStorageDir = mediaStorageDir + File.separator + "REC_" + timeStamp + ".3gp";

    	Log.d(TAG, "Uri calculated for audio recording: " + mediaStorageDir);
    	
    	return mediaStorageDir;
    }
    
    /**
     * Check if OS version has a http URLConnection bug. See here for more information:
     * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
     *
     * @return
     */
    //public static boolean hasHttpConnectionBug() {
    //    return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    //}

    /**
     * Check if OS version has built-in external cache dir method.
     *
     * @return
     */
    //public static boolean hasExternalCacheDir() {
    //    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    //}

    /**
     * Check if ActionBar is available.
     *
     * @return
     */
    //public static boolean hasActionBar() {
    //    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    //}
}