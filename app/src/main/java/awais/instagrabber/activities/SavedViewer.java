package awais.instagrabber.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.PostsAdapter;
import awais.instagrabber.asyncs.PostsFetcher;
import awais.instagrabber.asyncs.i.iLikedFetcher;
import awais.instagrabber.customviews.helpers.GridAutofitLayoutManager;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.ActivitySavedBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.interfaces.ItemGetter;
import awais.instagrabber.models.BasePostModel;
import awais.instagrabber.models.PostModel;
import awais.instagrabber.models.enums.DownloadMethod;
import awais.instagrabber.models.enums.ItemGetType;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Constants.AUTOLOAD_POSTS;
import static awais.instagrabber.utils.Utils.logCollector;

public final class SavedViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private static AsyncTask<?, ?, ?> currentlyExecuting;
    public static ItemGetter itemGetter;
    private PostsAdapter postsAdapter;
    private boolean hasNextPage, autoloadPosts;
    //private CommentModel commentModel;
    private ActivitySavedBinding savedBinding;
    private String action, username, endCursor;
    private final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
    private RecyclerLazyLoader lazyLoader;
    private Resources resources;
    private ArrayList<PostModel> selectedItems = new ArrayList<>();
    private final ArrayList<PostModel> allItems = new ArrayList<>();
    private MenuItem downloadAction;

    private final FetchListener<PostModel[]> postsFetchListener = new FetchListener<PostModel[]>() {
        @Override
        public void onResult(final PostModel[] result) {
            final int oldSize = allItems.size();
            if (result != null) {
                allItems.addAll(Arrays.asList(result));

                postsAdapter.notifyItemRangeInserted(oldSize, result.length);

                savedBinding.mainPosts.post(() -> {
                    savedBinding.mainPosts.setNestedScrollingEnabled(true);
                    savedBinding.mainPosts.setVisibility(View.VISIBLE);
                });

                final PostModel model = result[result.length - 1];
                if (model != null) {
                    endCursor = model.getEndCursor();

                    hasNextPage = model.hasNextPage();
                    if (autoloadPosts && hasNextPage && action.charAt(0) == '^')
                        currentlyExecuting = new iLikedFetcher(endCursor, postsFetchListener)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else if (autoloadPosts && hasNextPage)
                        currentlyExecuting = new PostsFetcher(action, endCursor, this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    else {
                        savedBinding.swipeRefreshLayout.setRefreshing(false);
                    }
                    model.setPageCursor(false, null);
                }
            }
            else {
                savedBinding.swipeRefreshLayout.setRefreshing(false);
                if (oldSize == 0) {
                    Toast.makeText(getApplicationContext(), R.string.empty_list, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedBinding = ActivitySavedBinding.inflate(getLayoutInflater());
        setContentView(savedBinding.getRoot());
        savedBinding.swipeRefreshLayout.setOnRefreshListener(this);
        autoloadPosts = Utils.settingsHelper.getBoolean(AUTOLOAD_POSTS);
        savedBinding.mainPosts.setNestedScrollingEnabled(false);
        final GridAutofitLayoutManager layoutManager = new GridAutofitLayoutManager(this, Utils.convertDpToPx(110));
        savedBinding.mainPosts.setLayoutManager(layoutManager);
        savedBinding.mainPosts.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(4)));

        final Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.EXTRAS_INDEX)
                || Utils.isEmpty((action = intent.getStringExtra(Constants.EXTRAS_INDEX)))
                || !intent.hasExtra(Constants.EXTRAS_USER)
                || Utils.isEmpty((username = intent.getStringExtra(Constants.EXTRAS_USER)))) {
            Utils.errorFinish(this);
            return;
        }

        savedBinding.mainPosts.setAdapter(postsAdapter = new PostsAdapter(allItems, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                final PostModel postModel = (PostModel) tag;

                if (postsAdapter.isSelecting) toggleSelection(postModel);
                else startActivity(new Intent(this, PostViewer.class)
                        .putExtra(Constants.EXTRAS_INDEX, postModel.getPosition())
                        .putExtra(Constants.EXTRAS_POST, postModel)
                        .putExtra(Constants.EXTRAS_USER, username)
                        .putExtra(Constants.EXTRAS_TYPE, ItemGetType.SAVED_ITEMS));
            }
        }, v -> {
            final Object tag = v.getTag();
            if (tag instanceof PostModel) {
                postsAdapter.isSelecting = true;
                toggleSelection((PostModel) tag);
            }
            return true;
        }));
        savedBinding.swipeRefreshLayout.setRefreshing(true);
        setSupportActionBar(savedBinding.toolbar.toolbar);
        savedBinding.toolbar.toolbar.setTitle((action.charAt(0) == '$' ? R.string.saved :
                (action.charAt(0) == '%' ? R.string.tagged : R.string.liked)));
        savedBinding.toolbar.toolbar.setSubtitle(username);

        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!autoloadPosts && hasNextPage) {
                savedBinding.swipeRefreshLayout.setRefreshing(true);
                stopCurrentExecutor();

                currentlyExecuting = action.charAt(0) == '^'
                    ? new iLikedFetcher(endCursor, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    : new PostsFetcher(action, endCursor, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                endCursor = null;
            }
        });
        savedBinding.mainPosts.addOnScrollListener(lazyLoader);

        itemGetter = itemGetType -> {
            if (itemGetType == ItemGetType.SAVED_ITEMS) return allItems;
            return null;
        };

        if (action.charAt(0) == '^') new iLikedFetcher(postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else new PostsFetcher(action, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.saved, menu);

        downloadAction = menu.findItem(R.id.downloadAction);
        downloadAction.setVisible(false);

        downloadAction.setOnMenuItemClickListener(item -> {
            if (selectedItems.size() > 0) {
                Utils.batchDownload(this, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
            }
            return true;
        });
        return true;
    }

    public void deselectSelection(final BasePostModel postModel) {
        if (postModel instanceof PostModel) {
            selectedItems.remove(postModel);
            postModel.setSelected(false);
            if (postsAdapter != null) notifyAdapter((PostModel) postModel);
        }
    }

    @Override
    public void onRefresh() {
        if (lazyLoader != null) lazyLoader.resetState();
        stopCurrentExecutor();
        allItems.clear();
        selectedItems.clear();
        if (postsAdapter != null) {
            postsAdapter.isSelecting = false;
            postsAdapter.notifyDataSetChanged();
        }
        savedBinding.swipeRefreshLayout.setRefreshing(true);
        if (action.charAt(0) == '^') new iLikedFetcher(postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else new PostsFetcher(action, postsFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED && selectedItems.size() > 0)
            Utils.batchDownload(this, null, DownloadMethod.DOWNLOAD_SAVED, selectedItems);
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

    private void toggleSelection(final PostModel postModel) {
        if (postModel != null && postsAdapter != null) {
            if (postModel.isSelected()) selectedItems.remove(postModel);
            else selectedItems.add(postModel);
            postModel.setSelected(!postModel.isSelected());
            notifyAdapter(postModel);
        }
    }

    private void notifyAdapter(final PostModel postModel) {
        if (selectedItems.size() < 1) postsAdapter.isSelecting = false;
        if (postModel.getPosition() < 0) postsAdapter.notifyDataSetChanged();
        else postsAdapter.notifyItemChanged(postModel.getPosition(), postModel);

        if (downloadAction != null) {
            downloadAction.setVisible(postsAdapter.isSelecting);
        }
    }
}