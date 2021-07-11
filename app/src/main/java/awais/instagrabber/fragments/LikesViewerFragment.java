package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import awais.instagrabber.adapters.LikesAdapter;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentLikesBinding;
import awais.instagrabber.repositories.responses.GraphQLUserListFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.MediaRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class LikesViewerFragment extends BottomSheetDialogFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LikesViewerFragment.class.getSimpleName();

    private FragmentLikesBinding binding;
    private RecyclerLazyLoader lazyLoader;
    private MediaRepository mediaRepository;
    private GraphQLRepository graphQLRepository;
    private boolean isLoggedIn;
    private String postId, endCursor;
    private boolean isComment;

    private final ServiceCallback<List<User>> cb = new ServiceCallback<List<User>>() {
        @Override
        public void onSuccess(final List<User> result) {
            final LikesAdapter likesAdapter = new LikesAdapter(result, v -> {
                final Object tag = v.getTag();
                if (tag instanceof User) {
                    User model = (User) tag;
                    try {
                        final NavDirections action = LikesViewerFragmentDirections.actionToProfile().setUsername(model.getUsername());
                        NavHostFragment.findNavController(LikesViewerFragment.this).navigate(action);
                    } catch (Exception e) {
                        Log.e(TAG, "onSuccess: ", e);
                    }
                }
            });
            binding.rvLikes.setAdapter(likesAdapter);
            binding.rvLikes.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.rvLikes.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            binding.swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "Error", t);
            try {
                final Context context = getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        }
    };

    private final ServiceCallback<GraphQLUserListFetchResponse> anonCb = new ServiceCallback<GraphQLUserListFetchResponse>() {
        @Override
        public void onSuccess(final GraphQLUserListFetchResponse result) {
            endCursor = result.getNextMaxId();
            final LikesAdapter likesAdapter = new LikesAdapter(result.getItems(), v -> {
                final Object tag = v.getTag();
                if (tag instanceof User) {
                    User model = (User) tag;
                    try {
                        final NavDirections action = LikesViewerFragmentDirections.actionToProfile().setUsername(model.getUsername());
                        NavHostFragment.findNavController(LikesViewerFragment.this).navigate(action);
                    } catch (Exception e) {
                        Log.e(TAG, "onSuccess: ", e);
                    }
                }
            });
            binding.rvLikes.setAdapter(likesAdapter);
            binding.swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "Error", t);
            try {
                final Context context = getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final long userId = CookieUtils.getUserIdFromCookie(cookie);
        isLoggedIn = !TextUtils.isEmpty(cookie) && userId != 0;
        // final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (csrfToken == null) return;
        mediaRepository = isLoggedIn ? MediaRepository.Companion.getInstance() : null;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        // setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = FragmentLikesBinding.inflate(getLayoutInflater());
        binding.swipeRefreshLayout.setEnabled(false);
        binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        init();
    }

    @Override
    public void onRefresh() {
        if (isComment && !isLoggedIn) {
            lazyLoader.resetState();
            graphQLRepository.fetchCommentLikers(
                    postId,
                    null,
                    CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            anonCb.onFailure(throwable);
                            return;
                        }
                        anonCb.onSuccess(response);
                    }), Dispatchers.getIO())
            );
        } else {
            mediaRepository.fetchLikes(
                    postId,
                    isComment,
                    CoroutineUtilsKt.getContinuation((users, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            cb.onFailure(throwable);
                            return;
                        }
                        //noinspection unchecked
                        cb.onSuccess((List<User>) users);
                    }), Dispatchers.getIO())
            );
        }
    }

    private void init() {
        if (getArguments() == null) return;
        final LikesViewerFragmentArgs fragmentArgs = LikesViewerFragmentArgs.fromBundle(getArguments());
        postId = fragmentArgs.getPostId();
        isComment = fragmentArgs.getIsComment();
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        binding.swipeRefreshLayout.setRefreshing(true);
        if (isComment && !isLoggedIn) {
            final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            binding.rvLikes.setLayoutManager(layoutManager);
            binding.rvLikes.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.HORIZONTAL));
            lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
                if (!TextUtils.isEmpty(endCursor)) {
                    graphQLRepository.fetchCommentLikers(
                            postId,
                            endCursor,
                            CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable != null) {
                                    anonCb.onFailure(throwable);
                                    return;
                                }
                                anonCb.onSuccess(response);
                            }), Dispatchers.getIO())
                    );
                }
                endCursor = null;
            });
            binding.rvLikes.addOnScrollListener(lazyLoader);
        }
        onRefresh();
    }
}