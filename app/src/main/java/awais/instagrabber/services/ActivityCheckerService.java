package awais.instagrabber.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.repositories.responses.notification.NotificationCounts;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.webservices.NewsService;
import awais.instagrabber.webservices.ServiceCallback;

public class ActivityCheckerService extends Service {
    private static final String TAG = "ActivityCheckerService";
    private static final int INITIAL_DELAY_MILLIS = 200;
    private static final int DELAY_MILLIS = 60000;

    private Handler handler;
    private NewsService newsService;
    private ServiceCallback<NotificationCounts> cb;
    private NotificationManagerCompat notificationManager;

    private final IBinder binder = new LocalBinder();
    private final Runnable runnable = () -> {
        newsService.fetchActivityCounts(cb);
    };

    public class LocalBinder extends Binder {
        public ActivityCheckerService getService() {
            return ActivityCheckerService.this;
        }
    }

    @Override
    public void onCreate() {
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        newsService = NewsService.getInstance();
        handler = new Handler();
        cb = new ServiceCallback<NotificationCounts>() {
            @Override
            public void onSuccess(final NotificationCounts result) {
                try {
                    if (result == null) return;
                    final List<String> notification = getNotificationString(result);
                    if (notification == null) return;
                    showNotification(notification);
                } finally {
                    handler.postDelayed(runnable, DELAY_MILLIS);
                }
            }

            @Override
            public void onFailure(final Throwable t) {}
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        startChecking();
        return binder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        stopChecking();
        return super.onUnbind(intent);
    }

    private void startChecking() {
        handler.postDelayed(runnable, INITIAL_DELAY_MILLIS);
    }

    private void stopChecking() {
        handler.removeCallbacks(runnable);
    }

    private List<String> getNotificationString(final NotificationCounts result) {
        final List<String> toReturn = new ArrayList<>(2);
        final List<String> list = new ArrayList<>();
        int count = 0;
        if (result.getRelationships() != 0) {
            list.add(getString(R.string.activity_count_relationship, result.getRelationships()));
            count += result.getRelationships();
        }
        if (result.getRequests() != 0) {
            list.add(getString(R.string.activity_count_requests, result.getRequests()));
            count += result.getRequests();
        }
        if (result.getUsertags() != 0) {
            list.add(getString(R.string.activity_count_usertags, result.getUsertags()));
            count += result.getUsertags();
        }
        if (result.getPhotosOfYou() != 0) {
            list.add(getString(R.string.activity_count_poy, result.getPhotosOfYou()));
            count += result.getPhotosOfYou();
        }
        if (result.getComments() != 0) {
            list.add(getString(R.string.activity_count_comments, result.getComments()));
            count += result.getComments();
        }
        if (result.getCommentLikes() != 0) {
            list.add(getString(R.string.activity_count_commentlikes, result.getCommentLikes()));
            count += result.getCommentLikes();
        }
        if (result.getLikes() != 0) {
            list.add(getString(R.string.activity_count_likes, result.getLikes()));
            count += result.getLikes();
        }
        if (list.isEmpty()) return null;
        toReturn.add(TextUtils.join(", ", list));
        toReturn.add(getResources().getQuantityString(R.plurals.activity_count_total, count, count));
        return toReturn;
    }

    private void showNotification(final List<String> notificationString) {
        final Notification notification = new NotificationCompat.Builder(this, Constants.ACTIVITY_CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setSmallIcon(R.drawable.ic_notif)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(notificationString.get(1))
                .setContentText(notificationString.get(0))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationString.get(0)))
                .setContentIntent(getPendingIntent())
                .build();
        notificationManager.notify(Constants.ACTIVITY_NOTIFICATION_ID, notification);
    }

    @NonNull
    private PendingIntent getPendingIntent() {
        final Intent intent = new Intent(getApplicationContext(), MainActivity.class)
                .setAction(Constants.ACTION_SHOW_ACTIVITY)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(getApplicationContext(), Constants.SHOW_ACTIVITY_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
