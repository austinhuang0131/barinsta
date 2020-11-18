package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import awais.instagrabber.models.FollowModel;
import awais.instagrabber.repositories.FriendshipRepository;
import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoListFetchResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FriendshipService extends BaseService {
    private static final String TAG = "FriendshipService";

    private final FriendshipRepository repository;

    private static FriendshipService instance;

    private FriendshipService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com")
                .build();
        repository = retrofit.create(FriendshipRepository.class);
    }

    public static FriendshipService getInstance() {
        if (instance == null) {
            instance = new FriendshipService();
        }
        return instance;
    }

    public void follow(final String userId,
                       final String targetUserId,
                       final String csrfToken,
                       final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("create", userId, targetUserId, csrfToken, callback);
    }

    public void unfollow(final String userId,
                         final String targetUserId,
                         final String csrfToken,
                         final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("destroy", userId, targetUserId, csrfToken, callback);
    }

    public void block(final String userId,
                      final String targetUserId,
                      final String csrfToken,
                      final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("block", userId, targetUserId, csrfToken, callback);
    }

    public void unblock(final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("unblock", userId, targetUserId, csrfToken, callback);
    }

    public void toggleRestrict(final String targetUserId,
                               final boolean restrict,
                               final String csrfToken,
                               final ServiceCallback<FriendshipRepoRestrictRootResponse> callback) {
        final Map<String, String> form = new HashMap<>(3);
        form.put("_csrftoken", csrfToken);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("target_user_id", targetUserId);
        final String action = restrict ? "restrict" : "unrestrict";
        final Call<FriendshipRepoRestrictRootResponse> request = repository.toggleRestrict(Constants.I_USER_AGENT, action, form);
        request.enqueue(new Callback<FriendshipRepoRestrictRootResponse>() {
            @Override
            public void onResponse(@NonNull final Call<FriendshipRepoRestrictRootResponse> call,
                                   @NonNull final Response<FriendshipRepoRestrictRootResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<FriendshipRepoRestrictRootResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    public void approve(final String userId,
                       final String targetUserId,
                       final String csrfToken,
                       final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("approve", userId, targetUserId, csrfToken, callback);
    }

    public void ignore(final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        change("ignore", userId, targetUserId, csrfToken, callback);
    }

    private void change(final String action,
                        final String userId,
                        final String targetUserId,
                        final String csrfToken,
                        final ServiceCallback<FriendshipRepoChangeRootResponse> callback) {
        final Map<String, Object> form = new HashMap<>(5);
        form.put("_csrftoken", csrfToken);
        form.put("_uid", userId);
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("radio_type", "wifi-none");
        form.put("user_id", targetUserId);
        final Map<String, String> signedForm = Utils.sign(form);
        final Call<FriendshipRepoChangeRootResponse> request = repository.change(Constants.I_USER_AGENT, action, targetUserId, signedForm);
        request.enqueue(new Callback<FriendshipRepoChangeRootResponse>() {
            @Override
            public void onResponse(@NonNull final Call<FriendshipRepoChangeRootResponse> call,
                                   @NonNull final Response<FriendshipRepoChangeRootResponse> response) {
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull final Call<FriendshipRepoChangeRootResponse> call,
                                  @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    // log in required
    public void getList(final boolean follower,
                        final String targetUserId,
                        final String maxId,
                        final ServiceCallback<FriendshipRepoListFetchResponse> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("max_id", maxId == null ? "" : maxId);
        final Call<String> request = repository.getList(Constants.I_USER_AGENT,
                                                        targetUserId,
                                                        follower ? "followers" : "following",
                                                        queryMap);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    if (callback == null) {
                        return;
                    }
                    final String body = response.body();
                    if (TextUtils.isEmpty(body)) {

                        callback.onSuccess(null);
                        return;
                    }
                    final FriendshipRepoListFetchResponse friendshipListFetchResponse = parseListResponse(body);
                    callback.onSuccess(friendshipListFetchResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }

    private FriendshipRepoListFetchResponse parseListResponse(@NonNull final String body) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final String nextMaxId = root.optString("next_max_id");
        final String status = root.optString("status");
        final JSONArray itemsJson = root.optJSONArray("users");
        final List<FollowModel> items = parseItems(itemsJson);
        return new FriendshipRepoListFetchResponse(
                nextMaxId,
                status,
                items
        );
    }

    private List<FollowModel> parseItems(final JSONArray items) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FollowModel> followModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FollowModel followModel = new FollowModel(itemJson.getString("pk"),
                                                            itemJson.getString("username"),
                                                            itemJson.optString("full_name"),
                                                            itemJson.getString("profile_pic_url"));
            if (followModel != null) {
                followModels.add(followModel);
            }
        }
        return followModels;
    }
}
