package awais.instagrabber.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.StoriesAdapter;
import awais.instagrabber.asyncs.PostFetcher;
import awais.instagrabber.asyncs.QuizAction;
import awais.instagrabber.asyncs.RespondAction;
import awais.instagrabber.asyncs.SeenAction;
import awais.instagrabber.asyncs.VoteAction;
import awais.instagrabber.asyncs.direct_messages.CreateThreadAction;
import awais.instagrabber.asyncs.direct_messages.DirectThreadBroadcaster;
import awais.instagrabber.customviews.helpers.SwipeGestureListener;
import awais.instagrabber.databinding.FragmentStoryViewerBinding;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.interfaces.SwipeEvent;
import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.HighlightModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.viewmodels.HighlightsViewModel;
import awais.instagrabber.viewmodels.StoriesViewModel;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesService;
import awaisomereport.LogCollector;

import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_THRESHOLD;
import static awais.instagrabber.customviews.helpers.SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD;
import static awais.instagrabber.utils.Constants.MARK_AS_SEEN;
import static awais.instagrabber.utils.Utils.logCollector;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class StoryViewerFragment extends Fragment {
    private static final String TAG = "StoryViewerFragment";

    private AppCompatActivity fragmentActivity;
    private View root;
    private @NonNull
    FragmentStoryViewerBinding binding;
    private String currentStoryUsername;
    private StoriesAdapter storiesAdapter;
    private SwipeEvent swipeEvent;
    private GestureDetectorCompat gestureDetector;
    private StoriesService storiesService;
    private StoryModel currentStory;
    private int slidePos;
    private int lastSlidePos;
    private String url;
    private PollModel poll;
    private QuestionModel question;
    private String[] mentions;
    private QuizModel quiz;
    private MenuItem menuDownload;
    private MenuItem menuDm;
    private SimpleExoPlayer player;
    private boolean isHashtag, isLoc;
    private String highlight;
    private boolean fetching = false;
    private int currentFeedStoryIndex;
    private StoriesViewModel storiesViewModel;
    private boolean shouldRefresh = true;
    private StoryViewerFragmentArgs fragmentArgs;
    private ViewModel viewModel;
    private boolean isHighlight;

    private final String cookie = settingsHelper.getString(Constants.COOKIE);

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) requireActivity();
        storiesService = StoriesService.getInstance();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentStoryViewerBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, final MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.story_menu, menu);
        menuDownload = menu.findItem(R.id.action_download);
        menuDm = menu.findItem(R.id.action_dms);
        menuDownload.setVisible(false);
        menuDm.setVisible(false);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
        // hide menu items from activity
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final Context context = getContext();
        if (context == null) return false;
        switch (item.getItemId()) {
            case R.id.action_download:
                if (ContextCompat.checkSelfPermission(context, DownloadUtils.PERMS[0]) == PackageManager.PERMISSION_GRANTED)
                    downloadStory();
                else
                    ActivityCompat.requestPermissions(requireActivity(), DownloadUtils.PERMS, 8020);
                return true;
            case R.id.action_dms:
                final EditText input = new EditText(context);
                input.setHint(R.string.reply_hint);
                new AlertDialog.Builder(context)
                        .setTitle(R.string.reply_story)
                        .setView(input)
                        .setPositiveButton(R.string.ok, (d, w) -> new CreateThreadAction(cookie, currentStory.getUserId(), threadId -> {
                            try {
                                final DirectThreadBroadcaster.StoryReplyBroadcastOptions options = new DirectThreadBroadcaster.StoryReplyBroadcastOptions(
                                        input.getText().toString(),
                                        currentStory.getStoryMediaId(),
                                        currentStory.getUserId()
                                );
                                final DirectThreadBroadcaster broadcast = new DirectThreadBroadcaster(threadId);
                                broadcast.setOnTaskCompleteListener(result -> Toast.makeText(
                                        context,
                                        result != null ? R.string.answered_story : R.string.downloader_unknown_error,
                                        Toast.LENGTH_SHORT
                                ).show());
                                broadcast.execute(options);
                            } catch (UnsupportedEncodingException e) {
                                Log.e(TAG, "Error", e);
                            }
                        }).execute())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            downloadStory();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        // reset subtitle
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
        super.onDestroy();
    }

    private void init() {
        if (getArguments() == null) return;
        fragmentArgs = StoryViewerFragmentArgs.fromBundle(getArguments());
        currentFeedStoryIndex = fragmentArgs.getFeedStoryIndex();
        highlight = fragmentArgs.getHighlight();
        isHighlight = !TextUtils.isEmpty(highlight);
        if (currentFeedStoryIndex >= 0) {
            viewModel = isHighlight
                        ? new ViewModelProvider(fragmentActivity).get(HighlightsViewModel.class)
                        : new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
        }
        // feedStoryModels = feedStoriesViewModel.getList().getValue();
        // feedStoryModels == null || feedStoryModels.isEmpty() ||
        setupStories();
    }

    private void setupStories() {
        storiesViewModel = new ViewModelProvider(this).get(StoriesViewModel.class);
        setupListeners();
        final Context context = getContext();
        if (context == null) return;
        binding.storiesList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        storiesAdapter = new StoriesAdapter((model, position) -> {
            currentStory = model;
            slidePos = position;
            refreshStory();
        });
        binding.storiesList.setAdapter(storiesAdapter);
        storiesViewModel.getList().observe(fragmentActivity, storiesAdapter::submitList);
        resetView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        final boolean hasFeedStories;
        List<?> models = null;
        if (currentFeedStoryIndex >= 0) {
            if (isHighlight) {
                final HighlightsViewModel highlightsViewModel = (HighlightsViewModel) viewModel;
                models = highlightsViewModel.getList().getValue();
                // final HighlightModel model = models.get(currentFeedStoryIndex);
                // currentStoryMediaId = model.getId();
                // currentStoryUsername = model.getTitle();
            } else {
                final FeedStoriesViewModel feedStoriesViewModel = (FeedStoriesViewModel) viewModel;
                models = feedStoriesViewModel.getList().getValue();
                // final FeedStoryModel model = models.get(currentFeedStoryIndex);
                // currentStoryMediaId = model.getStoryMediaId();
                // currentStoryUsername = model.getProfileModel().getUsername();
            }
        }
        hasFeedStories = models != null && !models.isEmpty();
        final List<?> finalModels = models;
        final Context context = getContext();
        if (context == null) return;
        swipeEvent = isRightSwipe -> {
            final List<StoryModel> storyModels = storiesViewModel.getList().getValue();
            final int storiesLen = storyModels == null ? 0 : storyModels.size();
            if (storiesLen <= 0) return;
            final boolean isLeftSwipe = !isRightSwipe;
            final boolean endOfCurrentStories = slidePos + 1 >= storiesLen;
            final boolean swipingBeyondCurrentStories = (endOfCurrentStories && isLeftSwipe) || (slidePos == 0 && isRightSwipe);
            if (swipingBeyondCurrentStories && hasFeedStories) {
                final int index = currentFeedStoryIndex;
                if (settingsHelper.getBoolean(MARK_AS_SEEN)) {
                    new SeenAction(cookie, currentStory).execute();
                }
                if ((isRightSwipe && index == 0) || (isLeftSwipe && index == finalModels.size() - 1)) {
                    Toast.makeText(context, R.string.no_more_stories, Toast.LENGTH_SHORT).show();
                    return;
                }
                final Object feedStoryModel = isRightSwipe
                                              ? finalModels.get(index - 1)
                                              : finalModels.size() == index + 1 ? null : finalModels.get(index + 1);
                paginateStories(feedStoryModel, context, isRightSwipe, currentFeedStoryIndex == finalModels.size() - 2);
                return;
            }
            if (isRightSwipe) {
                if (--slidePos <= 0) {
                    slidePos = 0;
                }
            } else if (++slidePos >= storiesLen) {
                slidePos = storiesLen - 1;
            }
            currentStory = storyModels.get(slidePos);
            refreshStory();
        };
        gestureDetector = new GestureDetectorCompat(context, new SwipeGestureListener(swipeEvent));
        binding.playerView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
                final float diffX = e2.getX() - e1.getX();
                try {
                    if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && Math.abs(diffX) > SWIPE_THRESHOLD
                            && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        swipeEvent.onSwipe(diffX > 0);
                        return true;
                    }
                } catch (final Exception e) {
                    if (logCollector != null)
                        logCollector.appendException(e, LogCollector.LogFile.ACTIVITY_STORY_VIEWER, "setupListeners",
                                                     new Pair<>("swipeEvent", swipeEvent),
                                                     new Pair<>("diffX", diffX));
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error", e);
                }
                return false;
            }
        };

        if (hasFeedStories) {
            binding.btnBackward.setVisibility(currentFeedStoryIndex == 0 ? View.INVISIBLE : View.VISIBLE);
            binding.btnForward.setVisibility(currentFeedStoryIndex == finalModels.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            binding.btnBackward.setOnClickListener(v -> paginateStories(finalModels.get(currentFeedStoryIndex - 1), context, true, false));
            binding.btnForward.setOnClickListener(v -> paginateStories(finalModels.get(currentFeedStoryIndex + 1), context, false,
                                                                       currentFeedStoryIndex == finalModels.size() - 2));
        }

        binding.imageViewer.setTapListener(simpleOnGestureListener);
        binding.spotify.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (tag instanceof CharSequence) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(tag.toString()));
                startActivity(intent);
            }
        });
        binding.viewStoryPost.setOnClickListener(v -> {
            final Object tag = v.getTag();
            if (!(tag instanceof CharSequence)) return;
            final String shortCode = tag.toString();
            final AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setView(R.layout.dialog_opening_post)
                    .create();
            alertDialog.show();
            new PostFetcher(shortCode, feedModel -> {
                final PostViewV2Fragment fragment = PostViewV2Fragment
                        .builder(feedModel)
                        .build();
                fragment.setOnShowListener(dialog -> alertDialog.dismiss());
                fragment.show(getChildFragmentManager(), "post_view");
            }).execute();
        });
        final View.OnClickListener storyActionListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof PollModel) {
                poll = (PollModel) tag;
                if (poll.getMyChoice() > -1) {
                    new AlertDialog.Builder(context).setTitle(R.string.voted_story_poll)
                                                    .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1,
                                                                                   new String[]{
                                                                                           (poll.getMyChoice() == 0 ? "√ " : "") + poll
                                                                                                   .getLeftChoice() + " (" + poll
                                                                                                   .getLeftCount() + ")",
                                                                                           (poll.getMyChoice() == 1 ? "√ " : "") + poll
                                                                                                   .getRightChoice() + " (" + poll
                                                                                                   .getRightCount() + ")"
                                                                                   }), null)
                                                    .setPositiveButton(R.string.ok, null)
                                                    .show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(poll.getQuestion())
                            .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new String[]{
                                    poll.getLeftChoice() + " (" + poll.getLeftCount() + ")",
                                    poll.getRightChoice() + " (" + poll.getRightCount() + ")"
                            }), (d, w) -> {
                                if (!TextUtils.isEmpty(cookie))
                                    new VoteAction(currentStory, poll, cookie, choice -> {
                                        if (choice > -1) {
                                            poll.setMyChoice(choice);
                                            Toast.makeText(context, R.string.votef_story_poll, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                    }).execute(w);
                            })
                            .setPositiveButton(R.string.cancel, null)
                            .show();
                }
            } else if (tag instanceof QuestionModel) {
                question = (QuestionModel) tag;
                final EditText input = new EditText(context);
                input.setHint(R.string.answer_hint);
                new AlertDialog.Builder(context)
                        .setTitle(question.getQuestion())
                        .setView(input)
                        .setPositiveButton(R.string.ok, (d, w) -> new RespondAction(currentStory, question, cookie, result -> {
                            if (result) {
                                Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                        }).execute(input.getText().toString()))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else if (tag instanceof String[]) {
                mentions = (String[]) tag;
                new AlertDialog.Builder(context)
                        .setTitle(R.string.story_mentions)
                        .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, mentions), (d, w) -> {
                            openProfile(mentions[w]);
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            } else if (tag instanceof QuizModel) {
                String[] choices = new String[quiz.getChoices().length];
                for (int q = 0; q < choices.length; ++q) {
                    choices[q] = (quiz.getMyChoice() == q ? "√ " : "") + quiz.getChoices()[q] + " (" + quiz.getCounts()[q] + ")";
                }
                new AlertDialog.Builder(context)
                        .setTitle(quiz.getMyChoice() > -1 ? getString(R.string.story_quizzed) : quiz.getQuestion())
                        .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, choices), (d, w) -> {
                            if (quiz.getMyChoice() == -1 && !TextUtils.isEmpty(cookie))
                                new QuizAction(currentStory, quiz, cookie, choice -> {
                                    if (choice > -1) {
                                        quiz.setMyChoice(choice);
                                        Toast.makeText(context, R.string.answered_story, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                }).execute(w);
                        })
                        .setPositiveButton(R.string.cancel, null)
                        .show();
            }
        };
        binding.poll.setOnClickListener(storyActionListener);
        binding.answer.setOnClickListener(storyActionListener);
        binding.mention.setOnClickListener(storyActionListener);
        binding.quiz.setOnClickListener(storyActionListener);
    }

    private void resetView() {
        slidePos = 0;
        lastSlidePos = 0;
        if (menuDownload != null) menuDownload.setVisible(false);
        if (menuDm != null) menuDm.setVisible(false);
        binding.imageViewer.setController(null);
        releasePlayer();
        String currentStoryMediaId = null;
        if (currentFeedStoryIndex >= 0) {
            if (isHighlight) {
                final HighlightsViewModel highlightsViewModel = (HighlightsViewModel) viewModel;
                final List<HighlightModel> models = highlightsViewModel.getList().getValue();
                if (models == null) return;
                final HighlightModel model = models.get(currentFeedStoryIndex);
                currentStoryMediaId = model.getId();
                currentStoryUsername = model.getTitle();
            } else {
                final FeedStoriesViewModel feedStoriesViewModel = (FeedStoriesViewModel) viewModel;
                final List<FeedStoryModel> models = feedStoriesViewModel.getList().getValue();
                if (models == null) return;
                final FeedStoryModel model = models.get(currentFeedStoryIndex);
                currentStoryMediaId = model.getStoryMediaId();
                currentStoryUsername = model.getProfileModel().getUsername();
            }
        } else if (!TextUtils.isEmpty(fragmentArgs.getProfileId()) && !TextUtils.isEmpty(fragmentArgs.getUsername())) {
            currentStoryMediaId = fragmentArgs.getProfileId();
            currentStoryUsername = fragmentArgs.getUsername();
        }
        isHashtag = fragmentArgs.getIsHashtag();
        isLoc = fragmentArgs.getIsLoc();
        final boolean hasUsername = !TextUtils.isEmpty(currentStoryUsername);
        if (hasUsername) {
            currentStoryUsername = currentStoryUsername.replace("@", "");
            final ActionBar actionBar = fragmentActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(currentStoryUsername);
            }
        }
        storiesViewModel.getList().setValue(Collections.emptyList());
        if (currentStoryMediaId == null) return;
        final ServiceCallback<List<StoryModel>> storyCallback = new ServiceCallback<List<StoryModel>>() {
            @Override
            public void onSuccess(final List<StoryModel> storyModels) {
                fetching = false;
                if (storyModels == null || storyModels.isEmpty()) {
                    storiesViewModel.getList().setValue(Collections.emptyList());
                    currentStory = null;
                    binding.storiesList.setVisibility(View.GONE);
                    return;
                }
                binding.storiesList.setVisibility(View.VISIBLE);
                storiesViewModel.getList().setValue(storyModels);
                currentStory = storyModels.get(0);
                refreshStory();
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error", t);
            }
        };
        storiesService.getUserStory(currentStoryMediaId,
                                    currentStoryUsername,
                                    isLoc,
                                    isHashtag,
                                    isHighlight,
                                    storyCallback);
    }

    private void refreshStory() {
        if (binding.storiesList.getVisibility() == View.VISIBLE) {
            final List<StoryModel> storyModels = storiesViewModel.getList().getValue();
            if (storyModels != null) {
                StoryModel item = storyModels.get(lastSlidePos);
                if (item != null) {
                    item.setCurrentSlide(false);
                    storiesAdapter.notifyItemChanged(lastSlidePos, item);
                }
                item = storyModels.get(slidePos);
                if (item != null) {
                    item.setCurrentSlide(true);
                    storiesAdapter.notifyItemChanged(slidePos, item);
                }
            }
        }
        lastSlidePos = slidePos;

        final MediaItemType itemType = currentStory.getItemType();

        if (menuDownload != null) menuDownload.setVisible(false);
        url = itemType == MediaItemType.MEDIA_TYPE_VIDEO ? currentStory.getVideoUrl() : currentStory.getStoryUrl();

        final String shortCode = currentStory.getTappableShortCode();
        binding.viewStoryPost.setVisibility(shortCode != null ? View.VISIBLE : View.GONE);
        binding.viewStoryPost.setTag(shortCode);

        final String spotify = currentStory.getSpotify();
        binding.spotify.setVisibility(spotify != null ? View.VISIBLE : View.GONE);
        binding.spotify.setTag(spotify);

        poll = currentStory.getPoll();
        binding.poll.setVisibility(poll != null ? View.VISIBLE : View.GONE);
        binding.poll.setTag(poll);

        question = currentStory.getQuestion();
        binding.answer.setVisibility((question != null && !TextUtils.isEmpty(cookie)) ? View.VISIBLE : View.GONE);
        binding.answer.setTag(question);

        mentions = currentStory.getMentions();
        binding.mention.setVisibility((mentions != null && mentions.length > 0) ? View.VISIBLE : View.GONE);
        binding.mention.setTag(mentions);

        quiz = currentStory.getQuiz();
        binding.quiz.setVisibility(quiz != null ? View.VISIBLE : View.GONE);
        binding.quiz.setTag(quiz);

        releasePlayer();
        if (isHashtag || isLoc) {
            final ActionBar actionBar = fragmentActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(currentStory.getUsername());
            }
        }
        if (itemType == MediaItemType.MEDIA_TYPE_VIDEO) setupVideo();
        else setupImage();

        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(Utils.datetimeParser.format(new Date(currentStory.getTimestamp() * 1000L)));
        }

        if (settingsHelper.getBoolean(MARK_AS_SEEN)) new SeenAction(cookie, currentStory).execute();
    }

    private void downloadStory() {
        final Context context = getContext();
        if (context == null) return;
        if (currentStory == null) {
            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadUtils.download(context, currentStory);
    }

    private void setupImage() {
        binding.progressView.setVisibility(View.VISIBLE);
        binding.playerView.setVisibility(View.GONE);
        binding.imageViewer.setVisibility(View.VISIBLE);
        final ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        final DraweeController controller = Fresco.newDraweeControllerBuilder()
                                                  .setImageRequest(requestBuilder)
                                                  .setOldController(binding.imageViewer.getController())
                                                  .setControllerListener(new BaseControllerListener<ImageInfo>() {

                                                      @Override
                                                      public void onFailure(final String id, final Throwable throwable) {
                                                          binding.progressView.setVisibility(View.GONE);
                                                      }

                                                      @Override
                                                      public void onFinalImageSet(final String id,
                                                                                  final ImageInfo imageInfo,
                                                                                  final Animatable animatable) {
                                                          if (menuDownload != null) {
                                                              menuDownload.setVisible(true);
                                                          }
                                                          if (currentStory.canReply() && menuDm != null && !TextUtils.isEmpty(cookie)) {
                                                              menuDm.setVisible(true);
                                                          }
                                                          binding.progressView.setVisibility(View.GONE);
                                                      }
                                                  })
                                                  .build();
        binding.imageViewer.setController(controller);
    }

    private void setupVideo() {
        binding.playerView.setVisibility(View.VISIBLE);
        binding.progressView.setVisibility(View.GONE);
        binding.imageViewer.setVisibility(View.GONE);
        binding.imageViewer.setController(null);

        final Context context = getContext();
        if (context == null) return;
        player = new SimpleExoPlayer.Builder(context).build();
        binding.playerView.setPlayer(player);
        player.setPlayWhenReady(settingsHelper.getBoolean(Constants.AUTOPLAY_VIDEOS));

        final Uri uri = Uri.parse(url);
        final MediaItem mediaItem = MediaItem.fromUri(uri);
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(context, "instagram"))
                .createMediaSource(mediaItem);
        mediaSource.addEventListener(new Handler(), new MediaSourceEventListener() {
            @Override
            public void onLoadCompleted(final int windowIndex,
                                        @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                        final LoadEventInfo loadEventInfo,
                                        final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                if (currentStory.canReply() && menuDm != null && !TextUtils.isEmpty(cookie))
                    menuDm.setVisible(true);
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadStarted(final int windowIndex,
                                      @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                      final LoadEventInfo loadEventInfo,
                                      final MediaLoadData mediaLoadData) {
                if (menuDownload != null) menuDownload.setVisible(true);
                if (currentStory.canReply() && menuDm != null && !TextUtils.isEmpty(cookie))
                    menuDm.setVisible(true);
                binding.progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadCanceled(final int windowIndex,
                                       @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                       final LoadEventInfo loadEventInfo,
                                       final MediaLoadData mediaLoadData) {
                binding.progressView.setVisibility(View.GONE);
            }

            @Override
            public void onLoadError(final int windowIndex,
                                    @Nullable final MediaSource.MediaPeriodId mediaPeriodId,
                                    final LoadEventInfo loadEventInfo,
                                    final MediaLoadData mediaLoadData,
                                    final IOException error,
                                    final boolean wasCanceled) {
                if (menuDownload != null) menuDownload.setVisible(false);
                if (menuDm != null) menuDm.setVisible(false);
                binding.progressView.setVisibility(View.GONE);
            }
        });
        player.setMediaSource(mediaSource);
        player.prepare();

        binding.playerView.setOnClickListener(v -> {
            if (player != null) {
                if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                player.setPlayWhenReady(player.getPlaybackState() == Player.STATE_ENDED || !player.isPlaying());
            }
        });
    }

    private void openProfile(final String username) {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
        final char t = username.charAt(0);
        if (t == '@') {
            final NavDirections action = HashTagFragmentDirections.actionGlobalProfileFragment(username);
            NavHostFragment.findNavController(this).navigate(action);
        } else if (t == '#') {
            final NavDirections action = HashTagFragmentDirections.actionGlobalHashTagFragment(username.substring(1));
            NavHostFragment.findNavController(this).navigate(action);
        } else {
            final NavDirections action = ProfileFragmentDirections.actionGlobalLocationFragment(username.split(" \\(")[1].replace(")", ""));
            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    private void releasePlayer() {
        if (player == null) return;
        try { player.stop(true); } catch (Exception ignored) { }
        try { player.release(); } catch (Exception ignored) { }
        player = null;
    }

    private void paginateStories(Object feedStory, Context context, boolean backward, boolean last) {
        if (feedStory != null) {
            if (fetching) {
                Toast.makeText(context, R.string.be_patient, Toast.LENGTH_SHORT).show();
                return;
            }
            fetching = true;
            binding.btnBackward.setVisibility(currentFeedStoryIndex == 1 && backward ? View.INVISIBLE : View.VISIBLE);
            binding.btnForward.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
            currentFeedStoryIndex = backward ? (currentFeedStoryIndex - 1) : (currentFeedStoryIndex + 1);
            resetView();
        }
    }
}
