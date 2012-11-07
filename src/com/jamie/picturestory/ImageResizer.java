package com.jamie.picturestory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * A simple subclass of {@link ImageWorker} that resizes images from resources given a target width
 * and height. Useful for when the input images might be too large to simply load directly into
 * memory.
 */
public class ImageResizer extends ImageWorker {
    private static final String TAG = "ImageWorker";
    protected int mImageWidth;
    protected int mImageHeight;

    /**
     * Initialize providing a single target image size (used for both width and height);
     *
     * @param context
     * @param imageWidth
     * @param imageHeight
     */
    public ImageResizer(Context context, int imageWidth, int imageHeight) {
        super(context);
        setImageSize(imageWidth, imageHeight);
    }

    /**
     * Initialize providing a single target image size (used for both width and height);
     *
     * @param context
     * @param imageSize
     */
    public ImageResizer(Context context, int imageSize) {
        super(context);
        setImageSize(imageSize);
    }

    /**
     * Set the target image width and height.
     *
     * @param width
     * @param height
     */
    public void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     * Set the target image size (width and height will be the same).
     *
     * @param size
     */
    public void setImageSize(int size) {
        setImageSize(size, size);
    }

    /**
     * The main processing method. This happens in a background task. In this case we are just
     * sampling down the bitmap and returning it from a resource.
     *
     * @param resId
     * @return
     */
    private Bitmap processBitmap(int resId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "processBitmap - " + resId);
        }
        return decodeSampledBitmapFromResource(
                mContext.getResources(), resId, mImageWidth, mImageHeight);
    }
    
    private Bitmap processBitmap(String file) {
    	return decodeSampledBitmapFromFile(file, mImageWidth, mImageHeight);
    }
    
    private Bitmap processBitmap(Uri uri) {
    	return decodeSampledBitmapFromUri(uri, mImageWidth, mImageHeight);
    }

    @Override
    protected Bitmap processBitmap(Object data) {
        String filepath = String.valueOf(data);
        String header = filepath.substring(0, 7);
        
        Log.d(TAG, "Header from filepth is: " + header);
        
        if (header.equals("content")) {
        	Log.d(TAG, "Filepath '" + filepath + "' detected as content uri.");
        	return processBitmap(Uri.parse(filepath));
        } else {
        	Log.d(TAG, "Filepath '" + filepath + "' detected as system file path.");
        	return processBitmap(filepath.substring(7));
        }
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res The resources object containing the image data
     * @param resId The resource id of the image data
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    
    public static synchronized Bitmap decodeSampledBitmapFromUri(Uri uri,
			int reqWidth, int reqHeight) {
		
		// Get InputStream for the Uri
    	InputStream input = null;
    	try {
        input = mContext.getContentResolver().openInputStream(uri);
    	}
    	catch (FileNotFoundException fnfe) {
    		Log.e(TAG, "File not found exception during image bounds decode. Uri: " 
    				+ uri.toString());
    		return null;
    	}

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        try {
        	input.close();
        }
        catch (IOException ioe) {
        	Log.e(TAG, "IO exception during image bounds decode. Uri: " + uri.toString());
        	return null;
        }
        
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Now decode bitmap with inSampleSize set
        try {
        	input = mContext.getContentResolver().openInputStream(uri);
        }
        catch (FileNotFoundException fnfe) {
        	Log.e(TAG, "File not found exception during image load. Uri: " + uri.toString());
        	return null;
        }
        
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        
        try {
        	input.close();
        }
        catch (IOException ioe) {
        	Log.e(TAG, "IO exception during image bounds decode. Uri: " + uri.toString());
        	return null;
        }
                
        return getRotation(bitmap, uri);
	}
    
    public static Bitmap getRotation(Bitmap bitmap, Uri uri) {
    	// Unfortunately we have to do an entire database query just to get the orientation
    	String[] projection = { MediaStore.Images.Media.ORIENTATION };
        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(projection[0]);
        cursor.moveToFirst();
        int orientation = cursor.getInt(column_index);
        cursor.close();
        
        Log.d(TAG, "Orientation detected from content resolver as: " + orientation);
        
        if (orientation > 0) {
        	Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
        	bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    	
    	return bitmap;
    }
    
    /**
     * Decode and sample down a bitmap from a file to the requested width and height.
     *
     * @param filename The full path of the file to decode
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static synchronized Bitmap decodeSampledBitmapFromFile(String filename,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        
        Bitmap bitmap = BitmapFactory.decodeFile(filename, options);
        return getRotation(bitmap, filename);
    }
    
    public static Bitmap getRotation(Bitmap bitmap, String filepath) {
    	ExifInterface exif = null;
    	try {
        	exif = new ExifInterface(filepath);
    	} catch (IOException ioe) {
    		Log.e(TAG, "IO Exception reading image EXIF data. Uri: " + filepath);
    	}
    	int orientationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
    	
    	int orientation;
    	switch(orientationCode) {
    	case ExifInterface.ORIENTATION_ROTATE_90:
    		orientation = 90;
    		break;
    	case ExifInterface.ORIENTATION_ROTATE_180:
    		orientation = 180;
    		break;
    	case ExifInterface.ORIENTATION_ROTATE_270:
    		orientation = 270;
    		break;
    	default:
    		orientation = 0;
    		break;
    	}
    	
    	Log.d(TAG, "Orientation detected from EXIF data as: " + orientation);
    	
    	if (orientation > 0) {
        	Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
        	bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    	
    	return bitmap;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger
            // inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down
            // further.
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }
}