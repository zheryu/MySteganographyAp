package com.zheryu.steganography_android;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created by Eric on 9/3/2015.
 */
public class ImageUtils {

    public static Bitmap getScaledBitmap(Bitmap bmp, int maxImageDimensions){
        int bitmapHeight = bmp.getHeight();
        int bitmapWidth = bmp.getWidth();
        int outHeight = bitmapHeight;
        int outWidth = bitmapWidth;
        if(bitmapWidth > bitmapHeight){
            outWidth = maxImageDimensions;
            outHeight = (maxImageDimensions * bitmapHeight) / bitmapWidth;
        }else if(bitmapWidth < bitmapHeight){
            outHeight = maxImageDimensions;
            outWidth = (maxImageDimensions * bitmapWidth) / bitmapHeight;
        }
        return Bitmap.createScaledBitmap(bmp, outWidth, outHeight, false);
    }

    public static Bitmap getUprightBitmap(Context context, Uri photoUri) throws IOException {
        Bitmap unrotated = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);

        int orientation = getOrientation(context, photoUri);

        /*
         * if the orientation is not 0 (or -1, which means we don't know), we
         * have to do a rotation.
         */
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            return Bitmap.createBitmap(unrotated, 0, 0, unrotated.getWidth(), unrotated.getHeight(), matrix, true);
        }
        return unrotated;
    }

    public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public static byte[] bitmapToByteArray(Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] intArrToByteArr(int[] inArr){
        ByteBuffer bb = ByteBuffer.allocate(inArr.length*4);
        IntBuffer ib = bb.asIntBuffer();
        ib.put(inArr);
        return bb.array();
    }

    public static int[] byteArrToIntArr(byte[] inArr){
        IntBuffer ib = ByteBuffer.wrap(inArr).asIntBuffer();
        int[] retArr = new int[ib.remaining()];
        ib.get(retArr);
        return retArr;
    }

}
