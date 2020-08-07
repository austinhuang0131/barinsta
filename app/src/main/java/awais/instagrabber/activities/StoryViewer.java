package awais.instagrabber.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;

import org.json.JSONObject;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.StoriesAdapter;
import awais.instagrabber.asyncs.DownloadAsync;
import awais.instagrabber.asyncs.i.iStoryStatusFetcher;
import awais.instagrabber.customviews.helpers.SwipeGestureListener;
import awais.instagrabber.databinding.ActivityStoryViewerBinding;
import awais.instagrabber.interfaces.SwipeEvent;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_THRESHOLD;
import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD;
import static awais.instagrabber.utils.Constants.FOLDER_PATH;
import static awais.instagrabber.utils.Constants.FOLDER_SAVE_TO;
import static awais.instagrabber.utils.Constants.MARK_AS_SEEN;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public final class StoryViewer extends BaseLanguageActivity {
    private final StoriesAdapter storiesAdapter = new StoriesAdapter(null, new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final Object tag = v.getTag();
            if (tag instanceof StoryModel) {
                currentStory = (StoryModel) tag;
                slidePos = currentStory.getPosition();
                refreshStory();
            }
        }
    });
    private ActivityStoryViewerBinding storyViewerBinding;
    private StoryModel[] storyModels;
    private GestureDetectorCompat gestureDetector;
    private SimpleExoPlayer player;
    private SwipeEvent swipeEvent;
    private MenuItem menuDownload, menuDm;
    private PollModel poll;
    private QuestionModel question;
    private String[] mentions;
    private QuizModel quiz;
    private StoryModel currentStory;
    private String url, username;
    private int slidePos = 0, lastSlidePos = 0;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storyViewerBinding = ActivityStoryViewerBinding.inflate(getLayoutInflater());
        setContentView(storyViewerBinding.getRoot());

        setSupportActionBar(storyViewerBinding.toolbar.toolbar);

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_STORIES)
                || (storyModels = (StoryModel[]) intent.getSerializableExtra(Constants.EXTRAS_STORIES)) == null) {
            Utils.errorFinish(this);
            return;
        }

        username = intent.getStringExtra(Constants.EXTRAS_USERNAME);
        final String highlight = intent.getStringExtra(Constants.EXTRAS_HIGHLIGHT);
        final boolean hasUsername = !Utils.isEmpty(username);
        final boolean hasHighlight = !Utils.isEmpty(highlight);

        if (hasUsername) {
            storyViewerBinding.toolbar.toolbar.setTitle(username);
            storyViewerBinding.toolbar.toolbar.setOnClickListener(v -> {
                searchUsername(username);
            });
            if (hasHighlight) storyViewerBinding.toolbar.toolbar.setSubtitle(getString(R.string.title_highlight, highlight));
            else storyViewerBinding.toolbar.toolbar.setSubtitle(R.string.title_user_story);
        }

        storyViewerBinding.storiesList.setVisibility(View.GONE);
        storyViewerBinding.storiesList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        storyViewerBinding.storiesList.setAdapter(storiesAdapter);

        swipeEvent = new SwipeEvent() {
            private final int storiesLen = storyModels != null ? storyModels.length : 0;

            @Override
            public void onSwipe(final boolean isRightSwipe) {
                if (storyModels != null && storiesLen > 0) {
                    if (((slidePos + 1 >= storiesLen && isRightSwipe == false) || (slidePos == 0 && isRightSwipe == true))
                            && intent.hasExtra(Constants.FEED)) {
                        final FeedStoryModel[] storyFeed = (FeedStoryModel[]) intent.getSerializableExtra(Constants.FEED);
                        final int index = intent.getIntExtra(Constants.FEED_ORDER, 1738);
                        if ((isRightSwipe == true && index == 0) || (isRightSwipe == false && index == storyFeed.length - 1))
                            Toast.makeText(getApplicationContext(), R.string.no_more_stories, Toast.LENGTH_SHORT).show();
                        else {
                            boolean fetching = false;
                            final FeedStoryModel feedStoryModel = isRightSwipe ?
                                    (index == 0 ? null : storyFeed[index - 1]) :
                                    (storyFeed.length == index + 1 ? null : storyFeed[index + 1]);
                            if (feedStoryModel != null) {
                                if (fetching) {
                                    Toast.makeText(getApplicationContext(), R.string.be_patient, Toast.LENGTH_SHORT).show();
                                } else {
                                    fetching = true;
                                    new iStoryStatusFetcher(feedStoryModel.getStoryMediaId(), null, false, false, result -> {
                                        if (result != null && result.length > 0) {
                                            final Intent newIntent = new Intent(getApplicationContext(), StoryViewer.class)
                                                    .putExtra(Constants.EXTRAS_STORIES, result)
                                                    .putExtra(Constants.EXTRAS_USERNAME, feedStoryModel.getProfileModel().getUsername())
                                                    .putExtra(Constants.FEED, storyFeed)
                                                    .putExtra(Constants.FEED_ORDER, isRightSwipe ? (index - 1) : (index + 1));
                                            newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(newIntent);
                                        } else
                                            Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            }
                        }
                    }
                    else {
                        if (isRightSwipe) {
                            if (--slidePos <= 0) slidePos = 0;
                        } else if (++slidePos >= storiesLen) slidePos = storiesLen - 1;
                        currentStory = storyModels[slidePos];
                        refreshStory();
                    }
                }
            }
        };
        gestureDetector = new GestureDetectorCompat(this, new SwipeGestureListener(swipeEvent));

        viewPost();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void viewPost() {
        lastSlidePos = 0;
        storyViewerBinding.storiesList.setVisibility(View.GONE);
        storiesAdapter.setData(null);

        if (menuDownload != null) menuDownload.setVisible(false);
        if (menuDm != null) menuDm.setVisible(false);

        storyViewerBinding.playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        storyViewerBinding.imageViewer.setOnSingleFlingListener((e1, e2, velocityX, velocityY) -> {
            final float diffX = e2.getX() - e1.getX();
            try {
                if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    swipeEvent.onSwipe(diffX > 0);
                    return true;
                }
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.ACTIVITY_STORY_VIEWER, "viewPost",
                            new Pair<>("swipeEvent", swipeEvent),
                            new Pair<>("diffX", diffX));
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
            return false;
        });

        storyViewerBinding.spotify.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(tag.toString()));
                startActivity(intent);
            }
        });

        storyViewerBinding.viewStoryPost.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) startActivity(new Intent(this, PostViewer.class)
                    .putExtra(Constants.EXTRAS_POST, new PostModel(tag.toString(), tag.toString().matches("^[\\d]+$"))));
        });

        final View.OnClickListener storyActionListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof PollModel) {
                poll = (PollModel) tag;
                if (poll.getMyChoice() > -1)
                    new AlertDialog.Builder(this).setTitle(R.string.voted_story_poll)
                            .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{
                                    (poll.getMyChoice() == 0 ? "√ " : "") + poll.getLeftChoice() + " (" + poll.getLeftCount() + ")",
                                    (poll.getMyChoice() == 1 ? "√ " : "") + poll.getRightChoice() + " (" + poll.getRightCount() + ")"
                            }), null)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                else new AlertDialog.Builder(this).setTitle(poll.getQuestion())
                        .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{
                                poll.getLeftChoice() + " (" + poll.getLeftCount() + ")",
                                poll.getRightChoice() + " (" + poll.getRightCount() + ")"
                        }), (d, w) -> {
                            new VoteAction().execute(w);
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            }
            else if (tag instanceof QuestionModel) {
                question = (QuestionModel) tag;
                final EditText input = new EditText(this);
                input.setHint(R.string.answer_hint);
                new AlertDialog.Builder(this).setTitle(question.getQuestion())
                        .setView(input)
                        .setPositiveButton(R.string.ok, (d,w) -> {
                            new RespondAction().execute(input.getText().toString());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
            else if (tag instanceof String[]) {
                mentions = (String[]) tag;
                new AlertDialog.Builder(this).setTitle(R.string.story_mentions)
                        .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mentions), (d,w) -> {
                            searchUsername(mentions[w]);
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            }
            else if (tag instanceof QuizModel) {
                quiz = (QuizModel) quiz;
                String[] choices = new String[quiz.getChoices().length];
                for (int q = 0; q < choices.length; ++q) {
                    choices[q] = (quiz.getMyChoice() == q ? "√ " :"") + quiz.getChoices()[q]+ " (" + String.valueOf(quiz.getCounts()[q]) + ")";
                }
                new AlertDialog.Builder(this).setTitle(quiz.getMyChoice() > -1 ? getString(R.string.story_quizzed) : quiz.getQuestion())
                        .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, choices), (d,w) -> {
                            if (quiz.getMyChoice() == -1) new QuizAction().execute(w);
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            }
        };

        storyViewerBinding.poll.setOnClickListener(storyActionListener);
        storyViewerBinding.answer.setOnClickListener(storyActionListener);
        storyViewerBinding.mention.setOnClickListener(storyActionListener);
        storyViewerBinding.quiz.setOnClickListener(storyActionListener);

        storiesAdapter.setData(storyModels);
        if (storyModels.length > 1) storyViewerBinding.storiesList.setVisibility(View.VISIBLE);

        currentStory = storyModels[0];
        refreshStory();
    }

    private void setupVideo() {
        storyViewerBinding.playerView.setVisibility(View.VISIBLE);
        storyViewerBinding.progressView.setVisibility(View.GONE);
        storyViewerBinding.imageViewer.setVisibility(View.GONE);
        storyViewerBinding.imageViewer.setImageDrawable(null);

        player = new SimpleExoPlayer.Builder(this).build();
        storyViewerBinding.playerView.setPlayer(player);
        player.setPlayWhenReady(settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));

        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, "instagram"))
                .createMediaSource(Uri.parse(url));
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                if (currentStory.canReply() && menuDm != null) menuDm.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                if (currentStory.canReply() && menuDm != null) menuDm.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData) {
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex, @Nullable final MediaSource.MediaPeriodId mediaPeriodId, final LoadEventInfo loadEventInfo, final MediaLoadData mediaLoadData, final IOException error, final boolean wasCanceled) {
                if (menuDownload != null) menuDownload.setVisible(false);
                if (menuDm != null) menuDm.setVisible(false);
                storyViewerBinding.progressView.setVisibility(View.GONE);
            }
        });
        player.prepare(mediaSource);

        storyViewerBinding.playerView.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                player.setPlayWhenReady(player.getPlaybackState() == Player.STATE_ENDED || !player.isPlaying());
            }
        });
    }

    private void setupImage() {
        storyViewerBinding.progressView.setVisibility(View.VISIBLE);
        storyViewerBinding.playerView.setVisibility(View.GONE);

        storyViewerBinding.imageViewer.setImageDrawable(null);
        storyViewerBinding.imageViewer.setVisibility(View.VISIBLE);
        storyViewerBinding.imageViewer.setZoomable(true);
        storyViewerBinding.imageViewer.setZoomTransitionDuration(420);
        storyViewerBinding.imageViewer.setMaximumScale(7.2f);

        Glide.with(this).load(url).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable final GlideException e, final Object model, final Target<Drawable> target, final boolean isFirstResource) {
                storyViewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(final Drawable resource, final Object model, final Target<Drawable> target, final DataSource dataSource, final boolean isFirstResource) {
                if (menuDownload != null) menuDownload.setVisible(true);
                if (currentStory.canReply() && menuDm != null) menuDm.setVisible(true);
                storyViewerBinding.progressView.setVisibility(View.GONE);
                return false;
            }
        }).into(storyViewerBinding.imageViewer);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        menuDownload = menu.findItem(R.id.action_download);
        menuDm = menu.findItem(R.id.action_dms);
        menuDownload.setVisible(true);
        menuDm.setVisible(false);
        menuDownload.setOnMenuItemClickListener(item -> {
            if (ContextCompat.checkSelfPermission(this, Utils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
                downloadStory();
            else
                ActivityCompat.requestPermissions(this, Utils.PERMS, 8020);
            return true;
        });
        menuDm.setOnMenuItemClickListener(item -> {
            final EditText input = new EditText(this);
            input.setHint(R.string.reply_hint);
            new AlertDialog.Builder(this).setTitle(R.string.reply_story)
                    .setView(input)
                    .setPositiveButton(R.string.ok, (d,w) -> {
                        new CommentAction().execute(input.getText().toString());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });

        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) downloadStory();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < 24) releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= 24) releasePlayer();
    }

    private void downloadStory() {
        int error = 0;
        if (currentStory != null) {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download");

            if (settingsHelper.getBoolean(FOLDER_SAVE_TO)) {
                final String customPath = settingsHelper.getString(FOLDER_PATH);
                if (!Utils.isEmpty(customPath)) dir = new File(customPath);
            }

            if (settingsHelper.getBoolean(Constants.DOWNLOAD_USER_FOLDER) && !Utils.isEmpty(username))
                dir = new File(dir, username);

            if (dir.exists() || dir.mkdirs()) {
                final String storyUrl = currentStory.getItemType() == MediaItemType.MEDIA_TYPE_VIDEO ? currentStory.getVideoUrl() : currentStory.getStoryUrl();
                final File saveFile = new File(dir, currentStory.getStoryMediaId() + "_" + currentStory.getTimestamp()
                        + Utils.getExtensionFromModel(storyUrl, currentStory));

                new DownloadAsync(this, storyUrl, saveFile, result -> {
                    final int toastRes = result != null && result.exists() ? R.string.downloader_complete
                            : R.string.downloader_error_download_file;
                    Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show();
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            } else error = 1;
        } else error = 2;

        if (error == 1) Toast.makeText(this, R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
        else if (error == 2) Toast.makeText(this, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
    }

    private void refreshStory() {
        if (storyViewerBinding.storiesList.getVisibility() == View.VISIBLE) {
            StoryModel item = storiesAdapter.getItemAt(lastSlidePos);
            if (item != null) {
                item.setCurrentSlide(false);
                storiesAdapter.notifyItemChanged(lastSlidePos, item);
            }

            item = storiesAdapter.getItemAt(slidePos);
            if (item != null) {
                item.setCurrentSlide(true);
                storiesAdapter.notifyItemChanged(slidePos, item);
            }
        }
        lastSlidePos = slidePos;

        final MediaItemType itemType = currentStory.getItemType();

        if (menuDownload != null) menuDownload.setVisible(false);
        url = itemType == MediaItemType.MEDIA_TYPE_VIDEO ? currentStory.getVideoUrl() : currentStory.getStoryUrl();

        final String shortCode = currentStory.getTappableShortCode();
        storyViewerBinding.viewStoryPost.setVisibility(shortCode != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.viewStoryPost.setTag(shortCode);

        final String spotify = currentStory.getSpotify();
        storyViewerBinding.spotify.setVisibility(spotify != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.spotify.setTag(spotify);

        poll = currentStory.getPoll();
        storyViewerBinding.poll.setVisibility(poll != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.poll.setTag(poll);

        question = currentStory.getQuestion();
        storyViewerBinding.answer.setVisibility(question != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.answer.setTag(question);

        mentions = currentStory.getMentions();
        storyViewerBinding.mention.setVisibility((mentions != null && mentions.length > 0) ? View.VISIBLE : View.GONE);
        storyViewerBinding.mention.setTag(mentions);

        quiz = currentStory.getQuiz();
        storyViewerBinding.quiz.setVisibility(quiz != null ? View.VISIBLE : View.GONE);
        storyViewerBinding.quiz.setTag(quiz);

        storyViewerBinding.toolbar.toolbar.setSubtitle(Utils.datetimeParser.format(new Date(currentStory.getTimestamp() * 1000L)));

        releasePlayer();
        final Intent intent = getIntent();
        if (intent.getBooleanExtra(Constants.EXTRAS_HASHTAG, false)) {
            storyViewerBinding.toolbar.toolbar.setTitle(currentStory.getUsername() + " (" + intent.getStringExtra(Constants.EXTRAS_USERNAME) + ")");
            storyViewerBinding.toolbar.toolbar.setOnClickListener(v -> {
                searchUsername(currentStory.getUsername());
            });
        }
        if (itemType == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
        else setupImage();

        if (settingsHelper.getBoolean(MARK_AS_SEEN)) new SeenAction().execute();
    }

    private void searchUsername(final String text) {
        if (Main.scanHack != null) {
            Main.scanHack.onResult(text);
            setResult(6969);
            Intent intent = new Intent(getApplicationContext(), Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.stop(true); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    public static int indexOfIntArray(Object[] array, Object key) {
        int returnvalue = -1;
        for (int i = 0; i < array.length; ++i) {
            if (key == array[i]) {
                returnvalue = i;
                break;
            }
        }
        return returnvalue;
    }

    class VoteAction extends AsyncTask<Integer, Void, Void> {
        int ok = -1;
        String action;

        protected Void doInBackground(Integer... rawchoice) {
            int choice = rawchoice[0];
            final String url = "https://www.instagram.com/media/"+currentStory.getStoryMediaId().split("_")[0]+"/"+poll.getId()+"/story_poll_vote/";
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                urlConnection.setRequestProperty("x-csrftoken",
                        settingsHelper.getString(Constants.COOKIE).split("csrftoken=")[1].split(";")[0]);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", "6");
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes("vote="+choice);
                wr.flush();
                wr.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = choice;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "vote: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok > -1) {
                poll.setMyChoice(ok);
                Toast.makeText(getApplicationContext(), R.string.votef_story_poll, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }

    class QuizAction extends AsyncTask<Integer, Void, Void> {
        int ok = -1;
        String action;

        protected Void doInBackground(Integer... rawchoice) {
            int choice = rawchoice[0];
            final String cookie = settingsHelper.getString(Constants.COOKIE);
            final String url = "https://i.instagram.com/api/v1/media/"+currentStory.getStoryMediaId().split("_")[0]+"/"+quiz.getId()+"/story_quiz_answer/";
            try {
                JSONObject ogbody = new JSONObject("{\"client_context\":\"" + UUID.randomUUID().toString()
                        +"\",\"mutation_token\":\"" + UUID.randomUUID().toString()
                        +"\",\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                        +"\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                        +"\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                        +"\"}");
                ogbody.put("answer", String.valueOf(choice));
                String urlParameters = Utils.sign(ogbody.toString());
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                Log.d("austin_debug", "quiz: "+url+" "+cookie+" "+urlParameters);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = choice;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "quiz: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok > -1) {
                quiz.setMyChoice(ok);
                Toast.makeText(getApplicationContext(), R.string.answered_story, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }

    class RespondAction extends AsyncTask<String, Void, Void> {
        boolean ok = false;
        String action;

        protected Void doInBackground(String... rawchoice) {
            final String cookie = settingsHelper.getString(Constants.COOKIE);
            final String url = "https://i.instagram.com/api/v1/media/"
                    +currentStory.getStoryMediaId().split("_")[0]+"/"+question.getId()+"/story_question_response/";
            try {
                JSONObject ogbody = new JSONObject("{\"client_context\":\"" + UUID.randomUUID().toString()
                        +"\",\"mutation_token\":\"" + UUID.randomUUID().toString()
                        +"\",\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                        +"\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                        +"\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                        +"\"}");
                String choice = rawchoice[0].replaceAll("\"", ("\\\""));
                ogbody.put("response", choice);
                String urlParameters = Utils.sign(ogbody.toString());
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    ok = true;
                }
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "respond: " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (ok) {
                Toast.makeText(getApplicationContext(), R.string.answered_story, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(getApplicationContext(), R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }

    class SeenAction extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... lmao) {
            final String cookie = settingsHelper.getString(Constants.COOKIE);
            final String url = "https://www.instagram.com/stories/reel/seen";
            try {
                String urlParameters = "reelMediaId="+currentStory.getStoryMediaId().split("_")[0]
                        +"&reelMediaOwnerId="+currentStory.getUserId()
                        +"&reelId="+currentStory.getUserId()
                        +"&reelMediaTakenAt="+String.valueOf(currentStory.getTimestamp())
                        +"&viewSeenAt="+String.valueOf(currentStory.getTimestamp());
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("x-csrftoken",
                        settingsHelper.getString(Constants.COOKIE).split("csrftoken=")[1].split(";")[0]);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                urlConnection.connect();
                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "seen: " + ex);
            }
            return null;
        }
    }

    class CommentAction extends AsyncTask<String, Void, Void> {
        boolean ok = false;

        protected Void doInBackground(String... rawAction) {
            final String action = rawAction[0];
            final String url = "https://i.instagram.com/api/v1/direct_v2/create_group_thread/";
            final String cookie = settingsHelper.getString(Constants.COOKIE);
            try {
                final HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                urlConnection.setUseCaches(false);
                final String urlParameters = Utils.sign("{\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                        +"\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                        +"\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                        +"\",\"recipient_users\":\"["+currentStory.getUserId() // <- string of array of number (not joking)
                        +"]\"}");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                urlConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try {
                        final String threadid = new JSONObject(Utils.readFromConnection(urlConnection)).getString("thread_id");
                        final String url2 = "https://i.instagram.com/api/v1/direct_v2/threads/broadcast/reel_share/";
                        final HttpURLConnection urlConnection2 = (HttpURLConnection) new URL(url2).openConnection();
                        urlConnection2.setRequestMethod("POST");
                        urlConnection2.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
                        urlConnection2.setUseCaches(false);
                        final String commentText = URLEncoder.encode(action, "UTF-8")
                                .replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
                                .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
                        final String cc = UUID.randomUUID().toString();
                        final String urlParameters2 = Utils.sign("{\"_csrftoken\":\"" + cookie.split("csrftoken=")[1].split(";")[0]
                                +"\",\"_uid\":\"" + Utils.getUserIdFromCookie(cookie)
                                +"\",\"__uuid\":\"" + settingsHelper.getString(Constants.DEVICE_UUID)
                                +"\",\"client_context\":\"" + cc
                                +"\",\"mutation_token\":\"" + cc
                                +"\",\"text\":\"" + commentText
                                +"\",\"media_id\":\"" + currentStory.getStoryMediaId()
                                +"\",\"reel_id\":\"" + currentStory.getUserId()
                                +"\",\"thread_ids\":\"["+threadid
                                +"]\",\"action\":\"send_item\",\"entry\":\"reel\"}");
                        urlConnection2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        urlConnection2.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters2.getBytes().length));
                        urlConnection2.setDoOutput(true);
                        DataOutputStream wr2 = new DataOutputStream(urlConnection2.getOutputStream());
                        wr2.writeBytes(urlParameters2);
                        wr2.flush();
                        wr2.close();
                        urlConnection2.connect();
                        Log.d("austin_debug", urlConnection2.getResponseCode() + " " + urlParameters2 + " " + cookie);
                        if (urlConnection2.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            ok = true;
                        }
                        urlConnection2.disconnect();
                    } catch (Throwable ex) {
                        Log.e("austin_debug", "reply (B): " + ex);
                    }
                }

                urlConnection.disconnect();
            } catch (Throwable ex) {
                Log.e("austin_debug", "reply (CT): " + ex);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(getApplicationContext(),
                    ok ? R.string.answered_story : R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
        }
    }
}