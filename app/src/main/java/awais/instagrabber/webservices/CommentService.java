package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import awais.instagrabber.models.Comment;
import awais.instagrabber.repositories.CommentRepository;
import awais.instagrabber.repositories.responses.ChildCommentsFetchResponse;
import awais.instagrabber.repositories.responses.CommentsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentService {
    private static final String TAG = "CommentService";

    private final CommentRepository repository;
    private final String deviceUuid, csrfToken;
    private final long userId;

    private static CommentService instance;

    private CommentService(final String deviceUuid,
                           final String csrfToken,
                           final long userId) {
        this.deviceUuid = deviceUuid;
        this.csrfToken = csrfToken;
        this.userId = userId;
        repository = RetrofitFactory.INSTANCE
                .getRetrofit()
                .create(CommentRepository.class);
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public long getUserId() {
        return userId;
    }

    public static CommentService getInstance(final String deviceUuid, final String csrfToken, final long userId) {
        if (instance == null
                || !Objects.equals(instance.getCsrfToken(), csrfToken)
                || !Objects.equals(instance.getDeviceUuid(), deviceUuid)
                || !Objects.equals(instance.getUserId(), userId)) {
            instance = new CommentService(deviceUuid, csrfToken, userId);
        }
        return instance;
    }

    public void fetchComments(@NonNull final String mediaId,
                              final String maxId,
                              @NonNull final ServiceCallback<CommentsFetchResponse> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("can_support_threading", "true");
        if (maxId != null) form.put("max_id", maxId);
        final Call<CommentsFetchResponse> request = repository.fetchComments(mediaId, form);
        request.enqueue(new Callback<CommentsFetchResponse>() {
            @Override
            public void onResponse(@NonNull final Call<CommentsFetchResponse> call, @NonNull final Response<CommentsFetchResponse> response) {
                final CommentsFetchResponse cfr = response.body();
                if (cfr == null) callback.onFailure(new Exception("response is empty"));
                callback.onSuccess(cfr);
            }

            @Override
            public void onFailure(@NonNull final Call<CommentsFetchResponse> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void fetchChildComments(@NonNull final String mediaId,
                                   @NonNull final String commentId,
                                   final String maxId,
                                   @NonNull final ServiceCallback<ChildCommentsFetchResponse> callback) {
        final Map<String, String> form = new HashMap<>();
        if (maxId != null) form.put("max_id", maxId);
        final Call<ChildCommentsFetchResponse> request = repository.fetchChildComments(mediaId, commentId, form);
        request.enqueue(new Callback<ChildCommentsFetchResponse>() {
            @Override
            public void onResponse(@NonNull final Call<ChildCommentsFetchResponse> call, @NonNull final Response<ChildCommentsFetchResponse> response) {
                final ChildCommentsFetchResponse cfr = response.body();
                if (cfr == null) callback.onFailure(new Exception("response is empty"));
                callback.onSuccess(cfr);
            }

            @Override
            public void onFailure(@NonNull final Call<ChildCommentsFetchResponse> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void comment(@NonNull final String mediaId,
                        @NonNull final String comment,
                        final String replyToCommentId,
                        @NonNull final ServiceCallback<Comment> callback) {
        final String module = "self_comments_v2";
        final Map<String, Object> form = new HashMap<>();
        // form.put("user_breadcrumb", userBreadcrumb(comment.length()));
        form.put("idempotence_token", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", deviceUuid);
        form.put("comment_text", comment);
        form.put("containermodule", module);
        if (!TextUtils.isEmpty(replyToCommentId)) {
            form.put("replied_to_comment_id", replyToCommentId);
        }
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentRequest = repository.comment(mediaId, signedForm);
        commentRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while creating comment");
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    // final String status = jsonObject.optString("status");
                    final JSONObject commentJsonObject = jsonObject.optJSONObject("comment");
                    Comment comment = null;
                    if (commentJsonObject != null) {
                        final JSONObject userJsonObject = commentJsonObject.optJSONObject("user");
                        if (userJsonObject != null) {
                            final Gson gson = new Gson();
                            final User user = gson.fromJson(userJsonObject.toString(), User.class);
                            comment = new Comment(
                                    commentJsonObject.optString("pk"),
                                    commentJsonObject.optString("text"),
                                    commentJsonObject.optLong("created_at"),
                                    0L,
                                    false,
                                    user,
                                    0
                            );
                        }
                    }
                    callback.onSuccess(comment);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void deleteComment(final String mediaId,
                              final String commentId,
                              @NonNull final ServiceCallback<Boolean> callback) {
        deleteComments(mediaId, Collections.singletonList(commentId), callback);
    }

    public void deleteComments(final String mediaId,
                               final List<String> commentIds,
                               @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("comment_ids_to_delete", android.text.TextUtils.join(",", commentIds));
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", deviceUuid);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> bulkDeleteRequest = repository.commentsBulkDelete(mediaId, signedForm);
        bulkDeleteRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while deleting comments");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                // Log.e(TAG, "Error deleting comments", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentLike(@NonNull final String commentId,
                            @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", deviceUuid);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentLikeRequest = repository.commentLike(commentId, signedForm);
        commentLikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while liking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error liking comment", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentUnlike(final String commentId,
                              @NonNull final ServiceCallback<Boolean> callback) {
        final Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", deviceUuid);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<String> commentUnlikeRequest = repository.commentUnlike(commentId, signedForm);
        commentUnlikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while unliking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error unliking comment", t);
                callback.onFailure(t);
            }
        });
    }

    public void translate(final String id,
                          @NonNull final ServiceCallback<String> callback) {
        final Map<String, String> form = new HashMap<>();
        form.put("id", String.valueOf(id));
        form.put("type", "2");
        final Call<String> request = repository.translate(form);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "Error occurred while translating");
                    callback.onSuccess(null);
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String translation = jsonObject.optString("translation");
                    callback.onSuccess(translation);
                } catch (JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                Log.e(TAG, "Error translating", t);
                callback.onFailure(t);
            }
        });
    }
}