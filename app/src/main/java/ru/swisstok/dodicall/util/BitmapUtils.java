package ru.swisstok.dodicall.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.ExifInterface;

import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    public static Bitmap createCircleBitmap(Bitmap source, boolean recycleSource) {
        final int size = Math.min(source.getWidth(), source.getHeight());
        final int center = size >> 1;
        final int radius = size >> 1;

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(center, center, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        int left = (size - source.getWidth()) >> 1;
        int top = (size - source.getHeight()) >> 1;

        canvas.drawBitmap(source, left, top, paint);

        if (recycleSource) {
            source.recycle();
        }

        return output;
    }

    public static Bitmap createRectangleBitmap(Bitmap source, boolean recycleSource) {
        final int minSide = Math.min(source.getWidth(), source.getHeight());

        Bitmap output = Bitmap.createBitmap(minSide, minSide, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        int left = (minSide - source.getWidth()) >> 1;
        int top = (minSide - source.getHeight()) >> 1;

        canvas.drawBitmap(source, left, top, null);

        if (recycleSource) {
            source.recycle();
        }

        return output;
    }

    public static int initPhotoRotation(String filePath) {
        int rotation = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int a = Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));
            if (a == (ExifInterface.ORIENTATION_ROTATE_90)) {
                rotation = 90;
            } else if (a == (ExifInterface.ORIENTATION_ROTATE_180)) {
                rotation = 180;
            } else if (a == (ExifInterface.ORIENTATION_ROTATE_270)) {
                rotation = 270;
            } else if (a == ExifInterface.ORIENTATION_NORMAL) {
                rotation = 0;
            } else if (a == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
                rotation = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotation;
    }

    public static String rotateImage(int degree, String imagePath) {
        if (degree <= 0) {
            return imagePath;
        }
        try {
            Bitmap b = BitmapFactory.decodeFile(imagePath);

            Matrix matrix = new Matrix();
            if (b.getWidth() > b.getHeight()) {
                matrix.setRotate(degree);
                Bitmap newBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                if (newBitmap != b) {
                    b.recycle();
                }
                b = newBitmap;
            }

            FileOutputStream fOut = new FileOutputStream(imagePath);
            String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            String imageType = imageName.substring(imageName.lastIndexOf(".") + 1);

            FileOutputStream out = new FileOutputStream(imagePath);
            if (imageType.equalsIgnoreCase("png")) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            } else if (imageType.equalsIgnoreCase("jpeg") || imageType.equalsIgnoreCase("jpg")) {
                b.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }

            fOut.flush();
            fOut.close();

            b.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imagePath;
    }


}
