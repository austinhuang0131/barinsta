package awais.instagrabber.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awais.instagrabber.adapters.FollowAdapter;
import awais.instagrabber.asyncs.FollowFetcher;
import awais.instagrabber.databinding.ActivityFollowBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.FollowModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;
import thoughtbot.expandableadapter.ExpandableGroup;

import static awais.instagrabber.utils.Utils.logCollector;

public final class FollowViewer extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private final ArrayList<FollowModel> followModels = new ArrayList<>();
    private final ArrayList<FollowModel> followingModels = new ArrayList<>();
    private final ArrayList<FollowModel> followersModels = new ArrayList<>();
    private final ArrayList<FollowModel> allFollowing = new ArrayList<>();
    private boolean followers, isCompare = false;
    private String id, name, namePost, type;
    private Resources resources;
    private FollowModel model;
    private FollowAdapter adapter;
    private View.OnClickListener clickListener;
    private ActivityFollowBinding followBinding;
    private AsyncTask<Void, Void, FollowModel[]> currentlyExecuting;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        followBinding = ActivityFollowBinding.inflate(getLayoutInflater());
        setContentView(followBinding.getRoot());

        final Intent intent = getIntent();
        if (intent == null || Utils.isEmpty(id = intent.getStringExtra(Constants.EXTRAS_ID))) {
            Utils.errorFinish(this);
            return;
        }

        setSupportActionBar(followBinding.toolbar.toolbar);

        followers = intent.getBooleanExtra(Constants.EXTRAS_FOLLOWERS, false);
        name = intent.getStringExtra(Constants.EXTRAS_NAME);
        namePost = name;
        if (Utils.isEmpty(name)) {
            // this usually should not occur
            name = "You";
            namePost = "You're";
        }

        followBinding.toolbar.toolbar.setTitle(name);

        resources = getResources();

        clickListener = v -> {
            final Object tag = v.getTag();
            if (tag instanceof FollowModel) {
                model = (FollowModel) tag;
                startActivity(
                        new Intent(getApplicationContext(), ProfileViewer.class)
                                .putExtra(Constants.EXTRAS_USERNAME, model.getUsername())
                );
            }
        };

        followBinding.swipeRefreshLayout.setOnRefreshListener(this);

        onRefresh();
    }

    @Override
    public void onRefresh() {
        if (isCompare) listCompare();
        else listFollows();
    }

    private void listFollows() {
        stopCurrentExecutor();

        type = resources.getString(followers ? R.string.followers_type_followers : R.string.followers_type_following);
        followBinding.toolbar.toolbar.setSubtitle(type);

        followModels.clear();

        final FetchListener<FollowModel[]> fetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void doBefore() {
                followBinding.swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onResult(final FollowModel[] result) {
                if (result == null) followBinding.swipeRefreshLayout.setRefreshing(false);
                else {
                    followModels.addAll(Arrays.asList(result));

                    final FollowModel model = result[result.length - 1];
                    if (model != null && model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(id, followers, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    } else {
                        followBinding.swipeRefreshLayout.setRefreshing(false);

                        refreshAdapter(followModels, null, null, null);
                    }
                }
            }
        };

        currentlyExecuting = new FollowFetcher(id, followers, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void listCompare() {
        stopCurrentExecutor();

        followBinding.toolbar.toolbar.setSubtitle(R.string.followers_compare);

        allFollowing.clear();
        followersModels.clear();
        followingModels.clear();

        final FetchListener<FollowModel[]> followingFetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void onResult(final FollowModel[] result) {
                if (result != null) {
                    followingModels.addAll(Arrays.asList(result));

                    final FollowModel model = result[result.length - 1];
                    if (model != null && model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(id, false, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    } else {
                        allFollowing.addAll(followersModels);
                        allFollowing.retainAll(followingModels);

                        for (final FollowModel followModel : allFollowing) {
                            followersModels.remove(followModel);
                            followingModels.remove(followModel);
                        }

                        allFollowing.trimToSize();
                        followersModels.trimToSize();
                        followingModels.trimToSize();

                        followBinding.swipeRefreshLayout.setRefreshing(false);

                        refreshAdapter(null, followingModels, followersModels, allFollowing);
                    }
                } else followBinding.swipeRefreshLayout.setRefreshing(false);
            }
        };
        final FetchListener<FollowModel[]> followersFetchListener = new FetchListener<FollowModel[]>() {
            @Override
            public void doBefore() {
                followBinding.swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onResult(final FollowModel[] result) {
                if (result != null) {
                    followersModels.addAll(Arrays.asList(result));
                    final FollowModel model = result[result.length - 1];
                    if (model == null || !model.hasNextPage()) {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(id, false, followingFetchListener)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        stopCurrentExecutor();
                        currentlyExecuting = new FollowFetcher(id, true, model.getEndCursor(), this)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        model.setPageCursor(false, null);
                    }
                }
            }
        };

        currentlyExecuting = new FollowFetcher(id, true, followersFetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.follow, menu);

        final MenuItem menuSearch = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            private final Filter filter = new Filter() {
//                private final ArrayList<FollowModel> searchFollowModels = new ArrayList<>(followModels.size() / 2);
//                private final ArrayList<FollowModel> searchFollowingModels = new ArrayList<>(followingModels.size() / 2);
//                private final ArrayList<FollowModel> searchFollowersModels = new ArrayList<>(followersModels.size() / 2);
//                private final ArrayList<FollowModel> searchAllFollowing = new ArrayList<>(allFollowing.size() / 2);
//
//                @Nullable
//                @Override
//                protected FilterResults performFiltering(@NonNull final CharSequence constraint) {
//                    searchFollowModels.clear();
//                    searchFollowingModels.clear();
//                    searchFollowersModels.clear();
//                    searchAllFollowing.clear();
//
//                    final int followModelsSize = followModels.size();
//                    final int followingModelsSize = followingModels.size();
//                    final int followersModelsSize = followersModels.size();
//                    final int allFollowingSize = allFollowing.size();
//
//                    int maxSize = followModelsSize;
//                    if (maxSize < followingModelsSize) maxSize = followingModelsSize;
//                    if (maxSize < followersModelsSize) maxSize = followersModelsSize;
//                    if (maxSize < allFollowingSize) maxSize = allFollowingSize;
//
//                    final String query = constraint.toString().toLowerCase();
//                    FollowModel followModel;
//                    while (maxSize != -1) {
//                        if (maxSize < followModelsSize) {
//                            followModel = followModels.get(maxSize);
//                            if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
//                                searchFollowModels.add(followModel);
//                        }
//
//                        if (maxSize < followingModelsSize) {
//                            followModel = followingModels.get(maxSize);
//                            if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
//                                searchFollowingModels.add(followModel);
//                        }
//
//                        if (maxSize < followersModelsSize) {
//                            followModel = followersModels.get(maxSize);
//                            if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
//                                searchFollowersModels.add(followModel);
//                        }
//
//                        if (maxSize < allFollowingSize) {
//                            followModel = allFollowing.get(maxSize);
//                            if (Utils.hasKey(query, followModel.getUsername(), followModel.getFullName()))
//                                searchAllFollowing.add(followModel);
//                        }
//
//                        --maxSize;
//                    }
//
//                    return null;
//                }
//
//                @Override
//                protected void publishResults(final CharSequence query, final FilterResults results) {
//                    refreshAdapter(searchFollowModels, searchFollowingModels, searchFollowersModels, searchAllFollowing);
//                }
//            };

            @Override
            public boolean onQueryTextSubmit(final String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String query) {
//                if (Utils.isEmpty(query)) refreshAdapter(followModels, followingModels, followersModels, allFollowing);
//                else filter.filter(query.toLowerCase());
                if (adapter != null) adapter.getFilter().filter(query);
                return true;
            }
        });

        final MenuItem menuCompare = menu.findItem(R.id.action_compare);
        menuCompare.setOnMenuItemClickListener(item -> {
            followBinding.rvFollow.setAdapter(null);
            if (isCompare) listFollows();
            else listCompare();
            isCompare = !isCompare;
            return true;
        });

        return true;
    }

    private void refreshAdapter(final ArrayList<FollowModel> followModels, final ArrayList<FollowModel> followingModels,
                                final ArrayList<FollowModel> followersModels, final ArrayList<FollowModel> allFollowing) {
        final ArrayList<ExpandableGroup> groups = new ArrayList<>(1);

        if (isCompare) {
            if (followingModels != null && followingModels.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_not_following, name), followingModels));
            if (followersModels != null && followersModels.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_not_follower, namePost), followersModels));
            if (allFollowing != null && allFollowing.size() > 0)
                groups.add(new ExpandableGroup(resources.getString(R.string.followers_both_following), allFollowing));
        } else {
            final ExpandableGroup group = new ExpandableGroup(type, followModels);
            groups.add(group);
        }

        adapter = new FollowAdapter(this, clickListener, groups);
        adapter.toggleGroup(0);
        followBinding.rvFollow.setAdapter(adapter);
    }

    public void stopCurrentExecutor() {
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
}