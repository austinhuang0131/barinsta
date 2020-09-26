package awais.instagrabber.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.util.Random;

public final class NumberUtils {
    @NonNull
    public static String millisToString(final long timeMs) {
        final long totalSeconds = timeMs / 1000;

        final long seconds = totalSeconds % 60;
        final long minutes = totalSeconds / 60 % 60;
        final long hours = totalSeconds / 3600;

        final String strSec = Long.toString(seconds);
        final String strMin = Long.toString(minutes);

        final String strRetSec = strSec.length() > 1 ? strSec : "0" + seconds;
        final String strRetMin = strMin.length() > 1 ? strMin : "0" + minutes;

        final String retMinSec = strRetMin + ':' + strRetSec;

        if (hours > 0)
            return Long.toString(hours) + ':' + retMinSec;
        return retMinSec;
    }

    public static int getResultingHeight(final int requiredWidth, final int height, final int width) {
        return requiredWidth * height / width;
    }

    public static int getResultingWidth(final int requiredHeight, final int height, final int width) {
        return requiredHeight * width / height;
    }

    public static long random(long origin, long bound) {
        final Random random = new Random();
        long r = random.nextLong();
        long n = bound - origin, m = n - 1;
        if ((n & m) == 0L)  // power of two
            r = (r & m) + origin;
        else if (n > 0L) {  // reject over-represented candidates
            //noinspection StatementWithEmptyBody
            for (long u = r >>> 1;            // ensure non-negative
                 u + m - (r = u % n) < 0L;    // rejection check
                 u = random.nextLong() >>> 1) // retry
                ;
            r += origin;
        } else {              // range not representable as long
            while (r < origin || r >= bound)
                r = random.nextLong();
        }
        return r;
    }

    @NonNull
    public static Pair<Integer, Integer> calculateWidthHeight(final int height, final int width, final int maxHeight, final int maxWidth) {
        int tempWidth = NumberUtils.getResultingWidth(maxHeight, height, width);
        int tempHeight = maxHeight;
        if (width > maxWidth) {
            tempHeight = NumberUtils.getResultingHeight(maxWidth, height, width);
            tempWidth = maxWidth;
        }
        return new Pair<>(tempWidth, tempHeight);
    }
}
