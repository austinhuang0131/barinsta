package awais.instagrabber.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.adapters.DirectMessagesAdapter;
import awais.instagrabber.asyncs.direct_messages.InboxFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.ActivityDmsBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.direct_messages.InboxModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;

public final class DirectMessages extends BaseLanguageActivity implements SwipeRefreshLayout.OnRefreshListener {
    private final ArrayList<InboxThreadModel> inboxThreadModelList = new ArrayList<>();
    private final DirectMessagesAdapter messagesAdapter = new DirectMessagesAdapter(inboxThreadModelList, v -> {
        final Object tag = v.getTag();
        if (tag instanceof InboxThreadModel) {
            startActivity(new Intent(this, DirectMessagesUserInbox.class)
                    .putExtra(Constants.EXTRAS_THREAD_MODEL, (InboxThreadModel) tag)
            );
        }
    });
    private final FetchListener<InboxModel> fetchListener = new FetchListener<InboxModel>() {
        @Override
        public void doBefore() {
            dmsBinding.swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxModel inboxModel) {
            if (inboxModel != null) {
                endCursor = inboxModel.getOldestCursor();
                if ("MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor)) endCursor = null;
                // todo get request / unseen count from inboxModel

                final InboxThreadModel[] threads = inboxModel.getThreads();
                if (threads != null && threads.length > 0) {
                    final int oldSize = inboxThreadModelList.size();
                    inboxThreadModelList.addAll(Arrays.asList(threads));

                    messagesAdapter.notifyItemRangeInserted(oldSize, threads.length);
                }
            }

            dmsBinding.swipeRefreshLayout.setRefreshing(false);
            stopCurrentExecutor();
        }
    };
    private String endCursor;
    private RecyclerLazyLoader lazyLoader;
    private AsyncTask<Void, Void, InboxModel> currentlyRunning;
    private ActivityDmsBinding dmsBinding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dmsBinding = ActivityDmsBinding.inflate(getLayoutInflater());
        setContentView(dmsBinding.getRoot());

        dmsBinding.swipeRefreshLayout.setOnRefreshListener(this);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        dmsBinding.rvDirectMessages.setLayoutManager(layoutManager);
        dmsBinding.rvDirectMessages.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        dmsBinding.rvDirectMessages.setAdapter(messagesAdapter);

        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!Utils.isEmpty(endCursor))
                currentlyRunning = new InboxFetcher(endCursor, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            endCursor = null;
        });

        dmsBinding.rvDirectMessages.addOnScrollListener(lazyLoader);

        stopCurrentExecutor();
        currentlyRunning = new InboxFetcher(null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onRefresh() {
        endCursor = null;
        lazyLoader.resetState();
        inboxThreadModelList.clear();
        messagesAdapter.notifyDataSetChanged();

        stopCurrentExecutor();
        currentlyRunning = new InboxFetcher(null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopCurrentExecutor() {
        if (currentlyRunning != null) {
            try {
                currentlyRunning.cancel(true);
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
            }
        }
    }
}