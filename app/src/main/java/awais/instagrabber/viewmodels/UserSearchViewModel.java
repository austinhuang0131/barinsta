package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.fragments.UserSearchMode;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.Debouncer;
import awais.instagrabber.utils.RankedRecipientsCache;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.DirectMessagesRepository;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class UserSearchViewModel extends ViewModel {
    private static final String TAG = UserSearchViewModel.class.getSimpleName();
    public static final String DEBOUNCE_KEY = "search";

    private String prevQuery;
    private String currentQuery;
    private Call<?> searchRequest;
    private long[] hideUserIds;
    private String[] hideThreadIds;
    private UserSearchMode searchMode;
    private boolean showGroups;
    private boolean waitingForCache;
    private boolean showCachedResults;

    private final MutableLiveData<Resource<List<RankedRecipient>>> recipients = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAction = new MutableLiveData<>(false);
    private final Debouncer<String> searchDebouncer;
    private final Set<RankedRecipient> selectedRecipients = new HashSet<>();
    private final UserRepository userRepository;
    private final DirectMessagesRepository directMessagesRepository;
    private final RankedRecipientsCache rankedRecipientsCache;

    public UserSearchViewModel() {
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        final long viewerId = CookieUtils.getUserIdFromCookie(cookie);
        final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        if (TextUtils.isEmpty(csrfToken) || viewerId <= 0 || TextUtils.isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("User is not logged in!");
        }
        userRepository = UserRepository.Companion.getInstance();
        directMessagesRepository = DirectMessagesRepository.Companion.getInstance();
        rankedRecipientsCache = RankedRecipientsCache.INSTANCE;
        if ((rankedRecipientsCache.isFailed() || rankedRecipientsCache.isExpired()) && !rankedRecipientsCache.isUpdateInitiated()) {
            updateRankedRecipientCache();
        }
        final Debouncer.Callback<String> searchCallback = new Debouncer.Callback<String>() {
            @Override
            public void call(final String key) {
                if (currentQuery != null && currentQuery.equalsIgnoreCase(prevQuery)) return;
                sendSearchRequest();
                prevQuery = currentQuery;
            }

            @Override
            public void onError(final Throwable t) {
                Log.e(TAG, "onError: ", t);
            }
        };
        searchDebouncer = new Debouncer<>(searchCallback, 1000);
    }

    private void updateRankedRecipientCache() {
        rankedRecipientsCache.setUpdateInitiated(true);
        directMessagesRepository.rankedRecipients(
                null,
                null,
                null,
                CoroutineUtilsKt.getContinuation((response, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "updateRankedRecipientCache: ", throwable);
                        rankedRecipientsCache.setUpdateInitiated(false);
                        rankedRecipientsCache.setFailed(true);
                        continueSearchIfRequired();
                        return;
                    }
                    rankedRecipientsCache.setResponse(response);
                    rankedRecipientsCache.setUpdateInitiated(false);
                    continueSearchIfRequired();
                }, Dispatchers.getIO())
        );
    }

    private void continueSearchIfRequired() {
        if (!waitingForCache) {
            if (showCachedResults) {
                recipients.postValue(Resource.success(getCachedRecipients()));
            }
            return;
        }
        waitingForCache = false;
        sendSearchRequest();
    }

    public LiveData<Resource<List<RankedRecipient>>> getRecipients() {
        return recipients;
    }

    public void search(@Nullable final String query) {
        currentQuery = query;
        if (TextUtils.isEmpty(query)) {
            cancelSearch();
            if (showCachedResults) {
                recipients.postValue(Resource.success(getCachedRecipients()));
            }
            return;
        }
        recipients.postValue(Resource.loading(getCachedRecipients()));
        searchDebouncer.call(DEBOUNCE_KEY);
    }

    private void sendSearchRequest() {
        if (!rankedRecipientsCache.isFailed()) { // to avoid infinite loop in case of any network issues
            if (rankedRecipientsCache.isUpdateInitiated()) {
                // wait for cache first
                waitingForCache = true;
                return;
            }
            if (rankedRecipientsCache.isExpired()) {
                // update cache first
                updateRankedRecipientCache();
                waitingForCache = true;
                return;
            }
        }
        switch (searchMode) {
            case RAVEN:
            case RESHARE:
                rankedRecipientSearch();
                break;
            case USER_SEARCH:
            default:
                defaultUserSearch();
                break;
        }
    }

    private void defaultUserSearch() {
        userRepository.search(currentQuery, CoroutineUtilsKt.getContinuation((userSearchResponse, throwable) -> {
            if (throwable != null) {
                Log.e(TAG, "onFailure: ", throwable);
                recipients.postValue(Resource.error(throwable.getMessage(), getCachedRecipients()));
                searchRequest = null;
                return;
            }
            if (userSearchResponse == null) {
                recipients.postValue(Resource.error(R.string.generic_null_response, getCachedRecipients()));
                searchRequest = null;
                return;
            }
            final List<RankedRecipient> list = userSearchResponse
                    .getUsers()
                    .stream()
                    .map(RankedRecipient::of)
                    .collect(Collectors.toList());
            recipients.postValue(Resource.success(mergeResponseWithCache(list)));
            searchRequest = null;
        }));
    }

    private void rankedRecipientSearch() {
        directMessagesRepository.rankedRecipients(
                searchMode.getMode(),
                showGroups,
                currentQuery,
                CoroutineUtilsKt.getContinuation((response, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "rankedRecipientSearch: ", throwable);
                        recipients.postValue(Resource.error(throwable.getMessage(), getCachedRecipients()));
                        return;
                    }
                    final List<RankedRecipient> list = response.getRankedRecipients();
                    if (list != null) {
                        recipients.postValue(Resource.success(mergeResponseWithCache(list)));
                    }
                }, Dispatchers.getIO())
        );
    }

    private List<RankedRecipient> mergeResponseWithCache(@NonNull final List<RankedRecipient> list) {
        final Iterator<RankedRecipient> iterator = list.stream()
                                                       .filter(Objects::nonNull)
                                                       .filter(this::filterValidRecipients)
                                                       .filter(this::filterOutGroups)
                                                       .filter(this::filterIdsToHide)
                                                       .iterator();
        return ImmutableList.<RankedRecipient>builder()
                .addAll(getCachedRecipients()) // add cached results first
                .addAll(iterator)
                .build();
    }

    @NonNull
    private List<RankedRecipient> getCachedRecipients() {
        final List<RankedRecipient> rankedRecipients = rankedRecipientsCache.getRankedRecipients();
        final List<RankedRecipient> list = rankedRecipients != null ? rankedRecipients : Collections.emptyList();
        return list.stream()
                   .filter(Objects::nonNull)
                   .filter(this::filterValidRecipients)
                   .filter(this::filterOutGroups)
                   .filter(this::filterQuery)
                   .filter(this::filterIdsToHide)
                   .collect(Collectors.toList());
    }

    private void handleErrorResponse(final Response<?> response, boolean updateResource) {
        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            if (updateResource) {
                recipients.postValue(Resource.error(R.string.generic_failed_request, getCachedRecipients()));
            }
            return;
        }
        String errorString;
        try {
            errorString = errorBody.string();
            Log.e(TAG, "handleErrorResponse: " + errorString);
        } catch (IOException e) {
            Log.e(TAG, "handleErrorResponse: ", e);
            errorString = e.getMessage();
        }
        if (updateResource) {
            recipients.postValue(Resource.error(errorString, getCachedRecipients()));
        }
    }

    public void cleanup() {
        searchDebouncer.terminate();
    }

    public void setSelectedRecipient(final RankedRecipient recipient, final boolean selected) {
        if (selected) {
            selectedRecipients.add(recipient);
        } else {
            selectedRecipients.remove(recipient);
        }
        showAction.postValue(!selectedRecipients.isEmpty());
    }

    public Set<RankedRecipient> getSelectedRecipients() {
        return selectedRecipients;
    }

    public void clearResults() {
        recipients.postValue(Resource.success(Collections.emptyList()));
        prevQuery = "";
    }

    public void cancelSearch() {
        searchDebouncer.cancel(DEBOUNCE_KEY);
        if (searchRequest != null) {
            searchRequest.cancel();
            searchRequest = null;
        }
    }

    public LiveData<Boolean> showAction() {
        return showAction;
    }

    public void setSearchMode(final UserSearchMode searchMode) {
        this.searchMode = searchMode;
    }

    public void setShowGroups(final boolean showGroups) {
        this.showGroups = showGroups;
    }

    public void setHideUserIds(final long[] hideUserIds) {
        if (hideUserIds != null) {
            final long[] copy = Arrays.copyOf(hideUserIds, hideUserIds.length);
            Arrays.sort(copy);
            this.hideUserIds = copy;
            return;
        }
        this.hideUserIds = null;
    }

    public void setHideThreadIds(final String[] hideThreadIds) {
        if (hideThreadIds != null) {
            final String[] copy = Arrays.copyOf(hideThreadIds, hideThreadIds.length);
            Arrays.sort(copy);
            this.hideThreadIds = copy;
            return;
        }
        this.hideThreadIds = null;
    }

    private boolean filterOutGroups(@NonNull RankedRecipient recipient) {
        // if showGroups is false, remove groups from the list
        if (showGroups || recipient.getThread() == null) {
            return true;
        }
        return !recipient.getThread().isGroup();
    }

    private boolean filterValidRecipients(@NonNull RankedRecipient recipient) {
        // check if both user and thread are null
        return recipient.getUser() != null || recipient.getThread() != null;
    }

    private boolean filterIdsToHide(@NonNull RankedRecipient recipient) {
        if (hideThreadIds != null && recipient.getThread() != null) {
            return Arrays.binarySearch(hideThreadIds, recipient.getThread().getThreadId()) < 0;
        }
        if (hideUserIds != null) {
            long pk = -1;
            if (recipient.getUser() != null) {
                pk = recipient.getUser().getPk();
            } else if (recipient.getThread() != null && !recipient.getThread().isGroup()) {
                final User user = recipient.getThread().getUsers().get(0);
                pk = user.getPk();
            }
            return Arrays.binarySearch(hideUserIds, pk) < 0;
        }
        return true;
    }

    private boolean filterQuery(@NonNull RankedRecipient recipient) {
        if (TextUtils.isEmpty(currentQuery)) {
            return true;
        }
        if (recipient.getThread() != null) {
            return recipient.getThread().getThreadTitle().toLowerCase().contains(currentQuery.toLowerCase());
        }
        return recipient.getUser().getUsername().toLowerCase().contains(currentQuery.toLowerCase())
                || recipient.getUser().getFullName().toLowerCase().contains(currentQuery.toLowerCase());
    }

    public void showCachedResults() {
        this.showCachedResults = true;
        if (rankedRecipientsCache.isUpdateInitiated()) return;
        recipients.postValue(Resource.success(getCachedRecipients()));
    }
}
