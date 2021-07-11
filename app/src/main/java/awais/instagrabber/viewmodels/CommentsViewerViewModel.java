package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import awais.instagrabber.R;
import awais.instagrabber.models.Comment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.ChildCommentsFetchResponse;
import awais.instagrabber.repositories.responses.CommentsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.CommentService;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class CommentsViewerViewModel extends ViewModel {
    private static final String TAG = CommentsViewerViewModel.class.getSimpleName();

    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentUserId = new MutableLiveData<>(0L);
    private final MutableLiveData<Resource<List<Comment>>> rootList = new MutableLiveData<>();
    private final MutableLiveData<Integer> rootCount = new MutableLiveData<>(0);
    private final MutableLiveData<Resource<List<Comment>>> replyList = new MutableLiveData<>();
    private final GraphQLRepository graphQLRepository;

    private String shortCode;
    private String postId;
    private String rootCursor;
    private boolean rootHasNext = true;
    private Comment repliesParent, replyTo;
    private String repliesCursor;
    private boolean repliesHasNext = true;
    private final CommentService commentService;
    private List<Comment> prevReplies;
    private String prevRepliesCursor;
    private boolean prevRepliesHasNext = true;

    private final ServiceCallback<CommentsFetchResponse> ccb = new ServiceCallback<CommentsFetchResponse>() {
        @Override
        public void onSuccess(final CommentsFetchResponse result) {
            // Log.d(TAG, "onSuccess: " + result);
            if (result == null) {
                rootList.postValue(Resource.error(R.string.generic_null_response, getPrevList(rootList)));
                return;
            }
            List<Comment> comments = result.getComments();
            if (rootCursor == null) {
                rootCount.postValue(result.getCommentCount());
            }
            if (rootCursor != null) {
                comments = mergeList(rootList, comments);
            }
            rootCursor = result.getNextMinId();
            rootHasNext = result.getHasMoreComments();
            rootList.postValue(Resource.success(comments));
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "onFailure: ", t);
            rootList.postValue(Resource.error(t.getMessage(), getPrevList(rootList)));
        }
    };
    private final ServiceCallback<ChildCommentsFetchResponse> rcb = new ServiceCallback<ChildCommentsFetchResponse>() {
        @Override
        public void onSuccess(final ChildCommentsFetchResponse result) {
            // Log.d(TAG, "onSuccess: " + result);
            if (result == null) {
                rootList.postValue(Resource.error(R.string.generic_null_response, getPrevList(replyList)));
                return;
            }
            List<Comment> comments = result.getChildComments();
            // Replies
            if (repliesCursor == null) {
                // add parent to top of replies
                comments = ImmutableList.<Comment>builder()
                        .add(repliesParent)
                        .addAll(comments)
                        .build();
            }
            if (repliesCursor != null) {
                comments = mergeList(replyList, comments);
            }
            repliesCursor = result.getNextMaxChildCursor();
            repliesHasNext = result.getHasMoreTailChildComments();
            replyList.postValue(Resource.success(comments));
        }

        @Override
        public void onFailure(final Throwable t) {
            Log.e(TAG, "onFailure: ", t);
            replyList.postValue(Resource.error(t.getMessage(), getPrevList(replyList)));
        }
    };

    public CommentsViewerViewModel() {
        graphQLRepository = GraphQLRepository.Companion.getInstance();
        final String cookie = settingsHelper.getString(Constants.COOKIE);
        final String deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID);
        final String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        final long userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        commentService = CommentService.getInstance(deviceUuid, csrfToken, userIdFromCookie);
    }

    public void setCurrentUser(final User currentUser) {
        isLoggedIn.postValue(currentUser != null);
        currentUserId.postValue(currentUser == null ? 0 : currentUser.getPk());
    }

    public void setPostDetails(final String shortCode, final String postId, final long postUserId) {
        this.shortCode = shortCode;
        this.postId = postId;
        fetchComments();
    }

    public LiveData<Boolean> isLoggedIn() {
        return isLoggedIn;
    }

    public LiveData<Long> getCurrentUserId() {
        return currentUserId;
    }

    @Nullable
    public Comment getRepliesParent() {
        return repliesParent;
    }

    @Nullable
    public void setReplyTo(final Comment replyTo) {
        this.replyTo = replyTo;
    }

    public LiveData<Resource<List<Comment>>> getRootList() {
        return rootList;
    }

    public LiveData<Resource<List<Comment>>> getReplyList() {
        return replyList;
    }

    public LiveData<Integer> getRootCommentsCount() {
        return rootCount;
    }

    public void fetchComments() {
        if (shortCode == null || postId == null) return;
        if (!rootHasNext) return;
        rootList.postValue(Resource.loading(getPrevList(rootList)));
        if (isLoggedIn.getValue()) {
            commentService.fetchComments(postId, rootCursor, ccb);
            return;
        }
        graphQLRepository.fetchComments(
                shortCode,
                true,
                rootCursor,
                enqueueRequest(true, shortCode, ccb)
        );
    }

    public void fetchReplies() {
        if (repliesParent == null) return;
        fetchReplies(repliesParent.getPk());
    }

    public void fetchReplies(@NonNull final String commentId) {
        if (!repliesHasNext) return;
        final List<Comment> list;
        if (repliesParent != null && !Objects.equals(repliesParent.getPk(), commentId)) {
            repliesCursor = null;
            repliesHasNext = false;
            list = Collections.emptyList();
        } else {
            list = getPrevList(replyList);
        }
        replyList.postValue(Resource.loading(list));
        final Boolean isLoggedInValue = isLoggedIn.getValue();
        if (isLoggedInValue != null && isLoggedInValue) {
            commentService.fetchChildComments(postId, commentId, repliesCursor, rcb);
            return;
        }
        graphQLRepository.fetchComments(commentId, false, repliesCursor, enqueueRequest(false, commentId, rcb));
    }

    private Continuation<String> enqueueRequest(final boolean root,
                                                final String shortCodeOrCommentId,
                                                @SuppressWarnings("rawtypes") final ServiceCallback callback) {
        return CoroutineUtilsKt.getContinuation((response, throwable) -> {
            if (throwable != null) {
                callback.onFailure(throwable);
                return;
            }
            if (response == null) {
                Log.e(TAG, "Error occurred while fetching gql comments of " + shortCodeOrCommentId);
                //noinspection unchecked
                callback.onSuccess(null);
                return;
            }
            try {
                final JSONObject body = root ? new JSONObject(response).getJSONObject("data")
                                                                       .getJSONObject("shortcode_media")
                                                                       .getJSONObject("edge_media_to_parent_comment")
                                             : new JSONObject(response).getJSONObject("data")
                                                                       .getJSONObject("comment")
                                                                       .getJSONObject("edge_threaded_comments");
                final int count = body.optInt("count");
                final JSONObject pageInfo = body.getJSONObject("page_info");
                final boolean hasNextPage = pageInfo.getBoolean("has_next_page");
                final String endCursor = pageInfo.isNull("end_cursor") || !hasNextPage ? null : pageInfo.optString("end_cursor");
                final JSONArray commentsJsonArray = body.getJSONArray("edges");
                final ImmutableList.Builder<Comment> builder = ImmutableList.builder();
                for (int i = 0; i < commentsJsonArray.length(); i++) {
                    final Comment commentModel = getComment(commentsJsonArray.getJSONObject(i).getJSONObject("node"), root);
                    builder.add(commentModel);
                }
                final Object result = root ? new CommentsFetchResponse(count, endCursor, builder.build(), hasNextPage)
                                           : new ChildCommentsFetchResponse(count, endCursor, builder.build(), hasNextPage);
                //noinspection unchecked
                callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "onResponse", e);
                callback.onFailure(e);
            }
        }, Dispatchers.getIO());
    }

    @NonNull
    private Comment getComment(@NonNull final JSONObject commentJsonObject, final boolean root) throws JSONException {
        final JSONObject owner = commentJsonObject.getJSONObject("owner");
        final User user = new User(
                owner.optLong(Constants.EXTRAS_ID, 0),
                owner.getString(Constants.EXTRAS_USERNAME),
                null,
                false,
                owner.getString("profile_pic_url"),
                owner.optBoolean("is_verified"));
        final JSONObject likedBy = commentJsonObject.optJSONObject("edge_liked_by");
        final String commentId = commentJsonObject.getString("id");
        final JSONObject childCommentsJsonObject = commentJsonObject.optJSONObject("edge_threaded_comments");
        int replyCount = 0;
        if (childCommentsJsonObject != null) {
            replyCount = childCommentsJsonObject.optInt("count");
        }
        return new Comment(commentId,
                           commentJsonObject.getString("text"),
                           commentJsonObject.getLong("created_at"),
                           likedBy != null ? likedBy.optLong("count", 0) : 0,
                           commentJsonObject.getBoolean("viewer_has_liked"),
                           user,
                           replyCount);
    }

    @NonNull
    private List<Comment> getPrevList(@NonNull final LiveData<Resource<List<Comment>>> list) {
        if (list.getValue() == null) return Collections.emptyList();
        final Resource<List<Comment>> listResource = list.getValue();
        if (listResource.data == null) return Collections.emptyList();
        return listResource.data;
    }

    private List<Comment> mergeList(@NonNull final LiveData<Resource<List<Comment>>> list,
                                    final List<Comment> comments) {
        final List<Comment> prevList = getPrevList(list);
        if (comments == null) {
            return prevList;
        }
        return ImmutableList.<Comment>builder()
                .addAll(prevList)
                .addAll(comments)
                .build();
    }

    public void showReplies(final Comment comment) {
        if (comment == null) return;
        if (repliesParent == null || !Objects.equals(repliesParent.getPk(), comment.getPk())) {
            repliesParent = comment;
            replyTo = comment;
            prevReplies = null;
            prevRepliesCursor = null;
            prevRepliesHasNext = true;
            fetchReplies(comment.getPk());
            return;
        }
        if (prevReplies != null && !prevReplies.isEmpty()) {
            // user clicked same comment, show prev loaded replies
            repliesCursor = prevRepliesCursor;
            repliesHasNext = prevRepliesHasNext;
            replyList.postValue(Resource.success(prevReplies));
            return;
        }
        // prev list was null or empty, fetch
        prevRepliesCursor = null;
        prevRepliesHasNext = true;
        fetchReplies(comment.getPk());
    }

    public LiveData<Resource<Object>> likeComment(@NonNull final Comment comment, final boolean liked, final boolean isReply) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        final ServiceCallback<Boolean> callback = new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result == null || !result) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                data.postValue(Resource.success(new Object()));
                setLiked(isReply, comment, liked);
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error liking comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        };
        if (liked) {
            commentService.commentLike(comment.getPk(), callback);
        } else {
            commentService.commentUnlike(comment.getPk(), callback);
        }
        return data;
    }

    private void setLiked(final boolean isReply,
                          @NonNull final Comment comment,
                          final boolean liked) {
        final List<Comment> list = getPrevList(isReply ? replyList : rootList);
        if (list == null) return;
        final List<Comment> copy = new ArrayList<>(list);
        OptionalInt indexOpt = IntStream.range(0, copy.size())
                                        .filter(i -> copy.get(i) != null && Objects.equals(copy.get(i).getPk(), comment.getPk()))
                                        .findFirst();
        if (!indexOpt.isPresent()) return;
        try {
            final Comment clone = (Comment) comment.clone();
            clone.setLiked(liked);
            copy.set(indexOpt.getAsInt(), clone);
            final MutableLiveData<Resource<List<Comment>>> liveData = isReply ? replyList : rootList;
            liveData.postValue(Resource.success(copy));
        } catch (Exception e) {
            Log.e(TAG, "setLiked: ", e);
        }
    }

    public LiveData<Resource<Object>> comment(@NonNull final String text,
                                              final boolean isReply) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        String replyToId = null;
        if (isReply && replyTo != null) {
            replyToId = replyTo.getPk();
        }
        if (isReply && replyToId == null) {
            data.postValue(Resource.error(null, null));
            return data;
        }
        commentService.comment(postId, text, replyToId, new ServiceCallback<Comment>() {
            @Override
            public void onSuccess(final Comment result) {
                if (result == null) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                addComment(result, isReply);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error during comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private void addComment(@NonNull final Comment comment, final boolean isReply) {
        final List<Comment> list = getPrevList(isReply ? replyList : rootList);
        final ImmutableList.Builder<Comment> builder = ImmutableList.builder();
        if (isReply) {
            // replies are added to the bottom of the list to preserve chronological order
            builder.addAll(list)
                   .add(comment);
        } else {
            builder.add(comment)
                   .addAll(list);
        }
        final MutableLiveData<Resource<List<Comment>>> liveData = isReply ? replyList : rootList;
        liveData.postValue(Resource.success(builder.build()));
    }

    public void translate(@NonNull final Comment comment,
                          @NonNull final ServiceCallback<String> callback) {
        commentService.translate(comment.getPk(), callback);
    }

    public LiveData<Resource<Object>> deleteComment(@NonNull final Comment comment, final boolean isReply) {
        final MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        commentService.deleteComment(postId, comment.getPk(), new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result == null || !result) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                removeComment(comment, isReply);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "Error deleting comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private void removeComment(@NonNull final Comment comment, final boolean isReply) {
        final List<Comment> list = getPrevList(isReply ? replyList : rootList);
        final List<Comment> updated = list.stream()
                                          .filter(Objects::nonNull)
                                          .filter(c -> !Objects.equals(c.getPk(), comment.getPk()))
                                          .collect(Collectors.toList());
        final MutableLiveData<Resource<List<Comment>>> liveData = isReply ? replyList : rootList;
        liveData.postValue(Resource.success(updated));
    }

    public void clearReplies() {
        prevRepliesCursor = repliesCursor;
        prevRepliesHasNext = repliesHasNext;
        repliesCursor = null;
        repliesHasNext = true;
        // cache prev reply list to save time and data if user clicks same comment again
        prevReplies = getPrevList(replyList);
        replyList.postValue(Resource.success(Collections.emptyList()));
    }
}
