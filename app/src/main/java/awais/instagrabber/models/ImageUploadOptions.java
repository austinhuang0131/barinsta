package awais.instagrabber.models;

import android.graphics.Bitmap;

public class ImageUploadOptions {
    private final Bitmap bitmap;
    private boolean isSidecar;
    private String waterfallId;

    public static class Builder {
        private Bitmap bitmap;
        private boolean isSidecar;
        private String waterfallId;

        public Builder(final Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public Builder setBitmap(final Bitmap bitmap) {
            this.bitmap = bitmap;
            return this;
        }

        public Builder setIsSidecar(final boolean isSidecar) {
            this.isSidecar = isSidecar;
            return this;
        }

        public Builder setWaterfallId(final String waterfallId) {
            this.waterfallId = waterfallId;
            return this;
        }

        public ImageUploadOptions build() {
            return new ImageUploadOptions(bitmap, isSidecar, waterfallId);
        }
    }

    public static Builder builder(final Bitmap file) {
        return new Builder(file);
    }

    private ImageUploadOptions(final Bitmap bitmap,
                               final boolean isSidecar,
                               final String waterfallId) {
        this.bitmap = bitmap;
        this.isSidecar = isSidecar;
        this.waterfallId = waterfallId;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public boolean isSidecar() {
        return isSidecar;
    }

    public String getWaterfallId() {
        return waterfallId;
    }
}
