package awais.instagrabber.fragments.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.motion.widget.MotionScene;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.asyncs.FeedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentFeedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.PostViewV2Fragment;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.webservices.StoriesRepository;
import kotlinx.coroutines.Dispatchers;

public class FeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FeedFragment";

    private MainActivity fragmentActivity;
    private MotionLayout root;
    private FragmentFeedBinding binding;
    private StoriesRepository storiesRepository;
    private boolean shouldRefresh = true;
    private final boolean isRotate = false;
    private FeedStoriesViewModel feedStoriesViewModel;
    private boolean storiesFetching;
    private ActionMode actionMode;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_POSTS_LAYOUT);
    private RecyclerView storiesRecyclerView;
    private MenuItem storyListMenu;

    private final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter(
            new FeedStoriesAdapter.OnFeedStoryClickListener() {
                @Override
                public void onFeedStoryClick(Story model, int position) {
                    final NavController navController = NavHostFragment.findNavController(FeedFragment.this);
                    if (isSafeToNavigate(navController)) {
                        final NavDirections action = FeedFragmentDirections
                                .actionFeedFragmentToStoryViewerFragment(StoryViewerOptions.forFeedStoryPosition(position));
                        navController.navigate(action);
                    }
                }

                @Override
                public void onFeedStoryLongClick(Story model, int position) {
                    navigateToProfile("@" + model.getUser().getUsername());
                }
            }
    );

    private final FeedAdapterV2.FeedItemCallback feedItemCallback = new FeedAdapterV2.FeedItemCallback() {
        @Override
        public void onPostClick(final Media feedModel, final View profilePicView, final View mainPostImage) {
            openPostDialog(feedModel, profilePicView, mainPostImage, -1);
        }

        @Override
        public void onSliderClick(final Media feedModel, final int position) {
            openPostDialog(feedModel, null, null, position);
        }

        @Override
        public void onCommentsClick(final Media feedModel) {
            try {
                final NavDirections commentsAction = FeedFragmentDirections.actionGlobalCommentsViewerFragment(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        feedModel.getUser().getPk()
                );
                NavHostFragment.findNavController(FeedFragment.this).navigate(commentsAction);
            } catch (Exception e) {
                Log.e(TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(final Media feedModel, final int childPosition, final View popupLocation) {
            final Context context = getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(final String hashtag) {
            final NavDirections action = FeedFragmentDirections.actionGlobalHashTagFragment(hashtag);
            NavHostFragment.findNavController(FeedFragment.this).navigate(action);
        }

        @Override
        public void onLocationClick(final Media feedModel) {
            final NavDirections action = FeedFragmentDirections.actionGlobalLocationFragment(feedModel.getLocation().getPk());
            NavHostFragment.findNavController(FeedFragment.this).navigate(action);
        }

        @Override
        public void onMentionClick(final String mention) {
            navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(final Media feedModel, final View profilePicView) {
            if (feedModel.getUser() == null) return;
            navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onProfilePicClick(final Media feedModel, final View profilePicView) {
            if (feedModel.getUser() == null) return;
            navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onURLClick(final String url) {
            Utils.openURL(getContext(), url);
        }

        @Override
        public void onEmailClick(final String emailId) {
            Utils.openEmailAddress(getContext(), emailId);
        }

        private void openPostDialog(final Media feedModel,
                                    final View profilePicView,
                                    final View mainPostImage,
                                    final int position) {
            // ViewCompat.setTransitionName(profilePicView, "profile_pic");
            // ViewCompat.setTransitionName(mainPostImage, "post_image");
            // final FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
            //         .addSharedElement(profilePicView, "profile_pic")
            //         .addSharedElement(mainPostImage, "post_image")
            //         .build();
            final NavController navController = NavHostFragment.findNavController(FeedFragment.this);
            final Bundle bundle = new Bundle();
            bundle.putSerializable(PostViewV2Fragment.ARG_MEDIA, feedModel);
            bundle.putInt(PostViewV2Fragment.ARG_SLIDER_POSITION, position);
            try {
                navController.navigate(R.id.action_global_post_view, bundle);
            } catch (Exception e) {
                Log.e(TAG, "openPostDialog: ", e);
            }
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            binding.feedRecyclerView.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(final ActionMode mode) {
                    binding.feedRecyclerView.endSelection();
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (FeedFragment.this.selectedFeedModels == null) return false;
                        final Context context = getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(FeedFragment.this.selectedFeedModels));
                        binding.feedRecyclerView.endSelection();
                        return true;
                    }
                    return false;
                }
            });
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!onBackPressedCallback.isEnabled()) {
                final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
                onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            }
            if (actionMode == null) {
                actionMode = fragmentActivity.startActionMode(multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(final Set<Media> selectedFeedModels) {
            final String title = getString(R.string.number_selected, selectedFeedModels.size());
            if (actionMode != null) {
                actionMode.setTitle(title);
            }
            FeedFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (onBackPressedCallback.isEnabled()) {
                onBackPressedCallback.setEnabled(false);
                onBackPressedCallback.remove();
            }
            if (actionMode != null) {
                actionMode.finish();
                actionMode = null;
            }
        }
    };

    private void navigateToProfile(final String username) {
        final NavController navController = NavHostFragment.findNavController(this);
        final Bundle bundle = new Bundle();
        bundle.putString("username", username);
        navController.navigate(R.id.action_global_profileFragment, bundle);
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        storiesRepository = StoriesRepository.Companion.getInstance();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        binding.feedSwipeRefreshLayout.setOnRefreshListener(this);
        /*
        FabAnimation.init(binding.fabCamera);
        FabAnimation.init(binding.fabStory);
        binding.fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRotate = FabAnimation.rotateFab(v, !isRotate);
                if (isRotate) {
                    FabAnimation.showIn(binding.fabCamera);
                    FabAnimation.showIn(binding.fabStory);
                }
                else {
                    FabAnimation.showOut(binding.fabCamera);
                    FabAnimation.showOut(binding.fabStory);
                }
            }
        });
         */
        setupFeedStories();
        setupFeed();
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.feed_menu, menu);
        storyListMenu = menu.findItem(R.id.storyList);
        storyListMenu.setVisible(!storiesFetching);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.storyList) {
            final NavDirections action = FeedFragmentDirections.actionGlobalStoryListViewerFragment("feed");
            NavHostFragment.findNavController(FeedFragment.this).navigate(action);
        } else if (item.getItemId() == R.id.layout) {
            showPostsLayoutPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.getRoot().postDelayed(feedStoriesAdapter::notifyDataSetChanged, 1000);
    }

    @Override
    public void onRefresh() {
        binding.feedRecyclerView.refresh();
        fetchStories();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (storiesRecyclerView != null) {
            fragmentActivity.removeCollapsingView(storiesRecyclerView);
        }
    }

    private void setupFeed() {
        binding.feedRecyclerView.setViewModelStoreOwner(this)
                                .setLifeCycleOwner(this)
                                .setPostFetchService(new FeedPostFetchService())
                                .setLayoutPreferences(layoutPreferences)
                                .addFetchStatusChangeListener(fetching -> updateSwipeRefreshState())
                                .setFeedItemCallback(feedItemCallback)
                                .setSelectionModeCallback(selectionModeCallback)
                                .init();
        binding.feedSwipeRefreshLayout.setRefreshing(true);
        binding.feedRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
                super.onScrolled(recyclerView, dx, dy);
                final boolean canScrollVertically = recyclerView.canScrollVertically(-1);
                final MotionScene.Transition transition = root.getTransition(R.id.transition);
                if (transition != null) {
                    transition.setEnable(!canScrollVertically);
                }
            }
        });
        // if (shouldAutoPlay) {
        //     videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
        //     binding.feedRecyclerView.addOnScrollListener(videoAwareRecyclerScroller);
        // }
    }

    private void updateSwipeRefreshState() {
        binding.feedSwipeRefreshLayout.setRefreshing(binding.feedRecyclerView.isFetching() || storiesFetching);
    }

    private void setupFeedStories() {
        if (storyListMenu != null) storyListMenu.setVisible(false);
        feedStoriesViewModel = new ViewModelProvider(fragmentActivity).get(FeedStoriesViewModel.class);
        final Context context = getContext();
        if (context == null) return;
        storiesRecyclerView = binding.header;
        storiesRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        storiesRecyclerView.setAdapter(feedStoriesAdapter);
        feedStoriesViewModel.getList().observe(getViewLifecycleOwner(), feedStoriesAdapter::submitList);
        fetchStories();
    }

    private void fetchStories() {
        if (storiesFetching) return;
        // final String cookie = settingsHelper.getString(Constants.COOKIE);
        storiesFetching = true;
        updateSwipeRefreshState();
        storiesRepository.getFeedStories(
                CoroutineUtilsKt.getContinuation((feedStoryModels, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(TAG, "failed", throwable);
                        storiesFetching = false;
                        updateSwipeRefreshState();
                        return;
                    }
                    storiesFetching = false;
                    //noinspection unchecked
                    feedStoriesViewModel.getList().postValue((List<Story>) feedStoryModels);
                    //noinspection unchecked
                    feedStoriesAdapter.submitList((List<Story>) feedStoryModels);
                    if (storyListMenu != null) storyListMenu.setVisible(true);
                    updateSwipeRefreshState();
                }), Dispatchers.getIO())
        );
    }

    private void showPostsLayoutPreferences() {
        final PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_POSTS_LAYOUT,
                preferences -> {
                    layoutPreferences = preferences;
                    new Handler().postDelayed(() -> binding.feedRecyclerView.setLayoutPreferences(preferences), 200);
                });
        fragment.show(getChildFragmentManager(), "posts_layout_preferences");
    }

    public void scrollToTop() {
        if (binding != null) {
            binding.feedRecyclerView.smoothScrollToPosition(0);
            // binding.storiesContainer.setExpanded(true);
        }
    }

    private boolean isSafeToNavigate(final NavController navController) {
        return navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.feedFragment;
    }
}
