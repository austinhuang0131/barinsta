package awais.instagrabber.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.i.iLikedFetcher;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentSavedBinding;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.PostsViewModel;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class SavedViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static AsyncTask<?, ?, ?> currentlyExecuting;
    private PostsAdapter postsAdapter;
    private boolean hasNextPage;
    private FragmentSavedBinding binding;
    private String username;
    private String endCursor;
    private RecyclerLazyLoader lazyLoader;
    private ArrayList<PostModel> selectedItems = new ArrayList<>();
    private ActionMode actionMode;
    private PostsViewModel postsViewModel;
    private LinearLayout root;
    private AppCompatActivity fragmentActivity;
    private boolean shouldRefresh = true;
    private PostItemType type;
    private String profileId;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            remove();
            if (postsAdapter == null) return;
            postsAdapter.clearSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(final ActionMode mode) {
                    onBackPressedCallback.handleOnBackPressed();
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (postsAdapter == null || username == null) {
                            return false;
                        }
                        final Context context = getContext();
                        if (context == null) return false;
                        DownloadUtils.batchDownload(context,
                                                    username,
                                                    DownloadMethod.DOWNLOAD_SAVED,
                                                    postsAdapter.getSelectedModels());
                        checkAndResetAction();
                        return true;
                    }
                    return false;
                }
            });
    private final FetchListener<List<PostModel>> postsFetchListener = new FetchListener<List<PostModel>>() {
        @Override
        public void onResult(final List<PostModel> result) {
            final List<PostModel> current = postsViewModel.getList().getValue();
            if (result != null && !result.isEmpty()) {
                if (current == null) {
                    postsViewModel.getList().postValue(result);
                } else {
                    final List<PostModel> currentCopy = new ArrayList<>(current);
                    currentCopy.addAll(result);
                    postsViewModel.getList().postValue(currentCopy);
                }
                binding.mainPosts.post(() -> {
                    binding.mainPosts.setNestedScrollingEnabled(true);
                    binding.mainPosts.setVisibility(View.VISIBLE);
                });

                final PostModel model = !result.isEmpty() ? result.get(result.size() - 1) : null;
                if (model != null) {
                    endCursor = model.getEndCursor();
                    hasNextPage = model.hasNextPage();
                    if (hasNextPage) {
                        fetchPosts();
                    } else {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    model.setPageCursor(false, null);
                }
            } else if (current == null) {
                final Context context = getContext();
                if (context == null) return;
                Toast.makeText(context, R.string.empty_list, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(SavedViewerFragment.this).popBackStack();
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    };
    private Observer<List<PostModel>> listObserver;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) getActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentSavedBinding.inflate(getLayoutInflater(), container, false);
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle();
        observeData();
    }

    private void observeData() {
        postsViewModel = new ViewModelProvider(this).get(PostsViewModel.class);
        postsViewModel.getList().removeObserver(listObserver);
        if (postsAdapter != null) {
            postsViewModel.getList().observe(getViewLifecycleOwner(), listObserver);
        }
    }

    private void init() {
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final SavedViewerFragmentArgs fragmentArgs = SavedViewerFragmentArgs.fromBundle(arguments);
        username = fragmentArgs.getUsername();
        profileId = fragmentArgs.getProfileId();
        type = fragmentArgs.getType();
        setTitle();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        // autoloadPosts = Utils.settingsHelper.getBoolean(AUTOLOAD_POSTS);
        binding.mainPosts.setNestedScrollingEnabled(false);
        final Context context = getContext();
        if (context == null) return;
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(context, Utils.convertDpToPx(110));
        binding.mainPosts.setLayoutManager(layoutManager);
        binding.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));
        postsAdapter = new PostsAdapter((postModel, position) -> {
            if (postsAdapter.isSelecting()) {
                if (actionMode == null) return;
                final String title = getString(R.string.number_selected, postsAdapter.getSelectedModels().size());
                actionMode.setTitle(title);
                return;
            }
            if (checkAndResetAction()) return;
            final List<PostModel> postModels = postsViewModel.getList().getValue();
            if (postModels == null || postModels.size() == 0) return;
            if (postModels.get(0) == null) return;
            final String postId = postModels.get(0).getPostId();
            final boolean isId = postId != null;
            final String[] idsOrShortCodes = new String[postModels.size()];
            for (int i = 0; i < postModels.size(); i++) {
                final PostModel tempPostModel = postModels.get(i);
                final String tempId = tempPostModel.getPostId();
                final String finalPostId = type == PostItemType.LIKED ? tempId.substring(0, tempId.indexOf("_")) : tempId;
                idsOrShortCodes[i] = isId ? finalPostId
                                          : tempPostModel.getShortCode();
            }
            final NavDirections action = ProfileFragmentDirections.actionGlobalPostViewFragment(
                    position,
                    idsOrShortCodes,
                    isId);
            NavHostFragment.findNavController(this).navigate(action);
        }, (model, position) -> {
            if (!postsAdapter.isSelecting()) {
                checkAndResetAction();
                return true;
            }
            final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
            if (onBackPressedCallback.isEnabled()) return true;
            actionMode = fragmentActivity.startActionMode(multiSelectAction);
            final String title = getString(R.string.number_selected, 1);
            actionMode.setTitle(title);
            onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), onBackPressedCallback);
            return true;
        });
        binding.mainPosts.setAdapter(postsAdapter);
        listObserver = list -> postsAdapter.submitList(list);
        observeData();
        binding.swipeRefreshLayout.setRefreshing(true);

        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (hasNextPage) {
                binding.swipeRefreshLayout.setRefreshing(true);
                fetchPosts();
                endCursor = null;
            }
        });
        binding.mainPosts.addOnScrollListener(lazyLoader);
        fetchPosts();
    }

    private void fetchPosts() {
        stopCurrentExecutor();
        final AsyncTask<Void, Void, List<PostModel>> asyncTask;
        switch (type) {
            case LIKED:
                asyncTask = new iLikedFetcher(endCursor, postsFetchListener);
                break;
            case SAVED:
            case TAGGED:
                if (TextUtils.isEmpty(profileId)) return;
                asyncTask = new PostsFetcher(profileId, type, endCursor, postsFetchListener);
                break;
            default:
                return;
        }
        currentlyExecuting = asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRefresh() {
        if (lazyLoader != null) lazyLoader.resetState();
        stopCurrentExecutor();
        endCursor = null;
        postsViewModel.getList().postValue(Collections.emptyList());
        selectedItems.clear();
        if (postsAdapter != null) {
            // postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
        }
        binding.swipeRefreshLayout.setRefreshing(true);
        fetchPosts();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED && selectedItems.size() > 0) {
            final Context context = getContext();
            if (context == null) return;
            DownloadUtils.batchDownload(context, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
        }
    }

    public static void stopCurrentExecutor() {
        if (currentlyExecuting != null) {
            try {
                currentlyExecuting.cancel(true);
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.MAIN_HELPER, "stopCurrentExecutor");
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
    }

    private void setTitle() {
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        final int titleRes;
        switch (type) {
            case SAVED:
                titleRes = R.string.saved;
                break;
            case LIKED:
                titleRes = R.string.liked;
                break;
            case TAGGED:
                titleRes = R.string.tagged;
                break;
            default:
                return; // no other types supported in this view
        }
        actionBar.setTitle(titleRes);
        actionBar.setSubtitle(username);
    }

    private boolean checkAndResetAction() {
        if (!onBackPressedCallback.isEnabled() && actionMode == null) {
            return false;
        }
        if (onBackPressedCallback.isEnabled()) {
            onBackPressedCallback.setEnabled(false);
            onBackPressedCallback.remove();
        }
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        return true;
    }
}