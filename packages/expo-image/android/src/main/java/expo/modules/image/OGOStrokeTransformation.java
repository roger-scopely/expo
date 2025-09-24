package expo.modules.image;

import android.content.Context;
import android.graphics.*;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Objects;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.*;
import jp.wasabeef.glide.transformations.BitmapTransformation;

public class OGOStrokeTransformation extends BitmapTransformation {
    private static final int VERSION = 1;
    private static final String ID = "OGOStrokeTransformation." + VERSION;
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    private final int color;
    private final int size;

    public OGOStrokeTransformation(int color, int size) {
        this.color = color;
        this.size = size;
    }

    @Override
    protected Bitmap transform(@NonNull Context context, @NonNull BitmapPool pool,
            @NonNull Bitmap toTransform, int outWidth, int outHeight) {

        GPUImageFilterGroup group = new GPUImageFilterGroup();
        // Map alpha channel into red channel
        group.addFilter(new GPUImageColorMatrixFilter(1, new float[] {
                0, 0, 0, 1,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 1,
        }));
        // This dilate filter only samples red channel, outputting 1 or 0
        group.addFilter(new GPUImageDilationFilter(size));
        // Now map red channel back to desired stroke colour
        group.addFilter(new GPUImageColorMatrixFilter(1, new float[] {
                Color.red(color) / 255f, 0, 0, 0,
                Color.green(color) / 255f, 0, 0, 0,
                Color.blue(color) / 255f, 0, 0, 0,
                1, 0, 0, 0,
        }));

        GPUImage gpu = new GPUImage(context);
        gpu.setImage(toTransform);
        gpu.setFilter(group);

        Bitmap bitmap = gpu.getBitmapWithFilterApplied();
        bitmap.setDensity(toTransform.getDensity());

        // Compose original over top dilated
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(toTransform, 0, 0, paint);

        return bitmap;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OGOStrokeTransformation)) {
            return false;
        }
        OGOStrokeTransformation other = (OGOStrokeTransformation) o;
        return color == other.color && size == other.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, size);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        messageDigest.update(ByteBuffer.allocate(8).putInt(color).putInt(size).array());
    }
}
