package awais.instagrabber.fragments.directmessages;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.adapters.DirectMessageInboxAdapter;
import awais.instagrabber.asyncs.direct_messages.InboxFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentDirectMessagesInboxBinding;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.direct_messages.InboxModel;
import awais.instagrabber.models.direct_messages.InboxThreadModel;
import awais.instagrabber.utils.Utils;

public class DirectMessageInboxFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "DirectMessagesInboxFrag";

    private FragmentActivity fragmentActivity;
    private SwipeRefreshLayout root;
    private RecyclerView inboxList;
    private RecyclerLazyLoader lazyLoader;
    private LinearLayoutManager layoutManager;
    private String endCursor;
    private AsyncTask<Void, Void, InboxModel> currentlyRunning;
    private InboxThreadModelListViewModel listViewModel;

    private final FetchListener<InboxModel> fetchListener = new FetchListener<InboxModel>() {
        @Override
        public void doBefore() {
            root.setRefreshing(true);
        }

        @Override
        public void onResult(final InboxModel inboxModel) {
            if (inboxModel != null) {
                endCursor = inboxModel.getOldestCursor();
                if ("MINCURSOR".equals(endCursor) || "MAXCURSOR".equals(endCursor))
                    endCursor = null;
                // todo get request / unseen count from inboxModel
                final InboxThreadModel[] threads = inboxModel.getThreads();
                if (threads != null && threads.length > 0) {
                    List<InboxThreadModel> list = listViewModel.getList().getValue();
                    list = list != null ? new LinkedList<>(list) : new LinkedList<>();
                    // final int oldSize = list != null ? list.size() : 0;
                    final List<InboxThreadModel> newList = Arrays.asList(threads);
                    list.addAll(newList);
                    listViewModel.getList().postValue(list);
                }
            }
            root.setRefreshing(false);
            stopCurrentExecutor();
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = requireActivity();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            return root;
        }
        final FragmentDirectMessagesInboxBinding binding = FragmentDirectMessagesInboxBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        root.setOnRefreshListener(this);
        inboxList = binding.inboxList;
        inboxList.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(requireContext());
        inboxList.setLayoutManager(layoutManager);
        final DirectMessageInboxAdapter inboxAdapter = new DirectMessageInboxAdapter(inboxThreadModel -> {
            final NavDirections action = DirectMessageInboxFragmentDirections.actionDMInboxFragmentToDMThreadFragment(inboxThreadModel.getThreadId(), inboxThreadModel.getThreadTitle());
            NavHostFragment.findNavController(this).navigate(action);
        });
        inboxList.setAdapter(inboxAdapter);
        listViewModel = new ViewModelProvider(fragmentActivity).get(InboxThreadModelListViewModel.class);
        listViewModel.getList().observe(fragmentActivity, inboxAdapter::submitList);
        initData();
        return root;
    }

    @Override
    public void onRefresh() {
        endCursor = null;
        lazyLoader.resetState();
        listViewModel.getList().postValue(Collections.emptyList());
        stopCurrentExecutor();
        currentlyRunning = new InboxFetcher(null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initData() {
        lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (!Utils.isEmpty(endCursor))
                currentlyRunning = new InboxFetcher(endCursor, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            endCursor = null;
        });
        inboxList.addOnScrollListener(lazyLoader);
        stopCurrentExecutor();
        currentlyRunning = new InboxFetcher(null, fetchListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopCurrentExecutor() {
        if (currentlyRunning != null) {
            try {
                currentlyRunning.cancel(true);
            } catch (final Exception e) {
                if (BuildConfig.DEBUG) Log.e(TAG, "", e);
            }
        }
    }

    public static class InboxThreadModelListViewModel extends ViewModel {
        private MutableLiveData<List<InboxThreadModel>> list;

        public MutableLiveData<List<InboxThreadModel>> getList() {
            if (list == null) {
                list = new MutableLiveData<>();
            }
            return list;
        }
    }
}
