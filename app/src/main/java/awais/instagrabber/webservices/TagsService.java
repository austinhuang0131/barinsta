package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.repositories.TagsRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TagsService extends BaseService {

    private static final String TAG = "TagsService";

    private static TagsService instance;

    private final TagsRepository webRepository;
    private final TagsRepository repository;

    private TagsService() {
        final Retrofit webRetrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com/")
                .build();
        webRepository = webRetrofit.create(TagsRepository.class);
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://i.instagram.com/")
                .build();
        repository = retrofit.create(TagsRepository.class);
    }

    public static TagsService getInstance() {
        if (instance == null) {
            instance = new TagsService();
        }
        return instance;
    }

    public void follow(@NonNull final String tag,
                       @NonNull final String csrfToken,
                       final ServiceCallback<Boolean> callback) {
        final Call<String> request = webRepository.follow(Constants.USER_AGENT,
                                                          csrfToken,
                                                          tag);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onFailure(new RuntimeException("body is null"));
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                // Log.e(TAG, "onFailure: ", t);
                callback.onFailure(t);
            }
        });
    }

    public void unfollow(@NonNull final String tag,
                         @NonNull final String csrfToken,
                         final ServiceCallback<Boolean> callback) {
        final Call<String> request = webRepository.unfollow(Constants.USER_AGENT,
                                                            csrfToken,
                                                            tag);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    callback.onFailure(new RuntimeException("body is null"));
                    return;
                }
                try {
                    final JSONObject jsonObject = new JSONObject(body);
                    final String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse: ", e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                // Log.e(TAG, "onFailure: ", t);
                callback.onFailure(t);
            }
        });
    }

    public void fetchPosts(@NonNull final String tag,
                           final String maxId,
                           final ServiceCallback<TagPostsFetchResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<String> request = repository.fetchPosts(tag, builder.build());
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
                    final TagPostsFetchResponse tagPostsFetchResponse = parseResponse(body);
                    callback.onSuccess(tagPostsFetchResponse);
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

    private TagPostsFetchResponse parseResponse(@NonNull final String body) throws JSONException {
        final JSONObject root = new JSONObject(body);
        final boolean moreAvailable = root.optBoolean("more_available");
        final String nextMaxId = root.optString("next_max_id");
        final int numResults = root.optInt("num_results");
        final String status = root.optString("status");
        final JSONArray itemsJson = root.optJSONArray("items");
        final List<FeedModel> items = parseItems(itemsJson);
        return new TagPostsFetchResponse(
                moreAvailable,
                nextMaxId,
                numResults,
                status,
                items
        );
    }

    private List<FeedModel> parseItems(final JSONArray items) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return feedModels;
    }

    public void fetchGraphQLPosts(@NonNull final String tag,
                           final String maxId,
                           final ServiceCallback<TagPostsFetchResponse> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "9b498c08113f1e09617a1703c22b2f32");
        queryMap.put("variables", "{" +
                "\"tag_name\":\"" + tag + "\"," +
                "\"first\":25," +
                "\"after\":\"" + (maxId == null ? "" : maxId) + "\"" +
                "}");
        final Call<String> request = webRepository.fetchGraphQLPosts(queryMap);
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
                    final TagPostsFetchResponse tagPostsFetchResponse = parseGraphQLResponse(body);
                    callback.onSuccess(tagPostsFetchResponse);
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

    private TagPostsFetchResponse parseGraphQLResponse(@NonNull final String body) throws JSONException {
        final JSONObject rootroot = new JSONObject(body);
        final JSONObject root = rootroot.getJSONObject("data").getJSONObject("hashtag").getJSONObject("edge_hashtag_to_media");
        final boolean moreAvailable = root.getJSONObject("page_info").optBoolean("has_next_page");
        final String nextMaxId = root.getJSONObject("page_info").optString("end_cursor");
        final int numResults = root.optInt("count");
        final String status = rootroot.optString("status");
        final JSONArray itemsJson = root.optJSONArray("edges");
        final List<FeedModel> items = parseGraphQLItems(itemsJson);
        return new TagPostsFetchResponse(
                moreAvailable,
                nextMaxId,
                numResults,
                status,
                items
        );
    }

    private List<FeedModel> parseGraphQLItems(final JSONArray items) throws JSONException {
        if (items == null) {
            return Collections.emptyList();
        }
        final List<FeedModel> feedModels = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            final JSONObject itemJson = items.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseGraphQLItem(itemJson);
            if (feedModel != null) {
                feedModels.add(feedModel);
            }
        }
        return feedModels;
    }

    public static class TagPostsFetchResponse {
        private boolean moreAvailable;
        private String nextMaxId;
        private int numResults;
        private String status;
        private List<FeedModel> items;

        public TagPostsFetchResponse(final boolean moreAvailable,
                                     final String nextMaxId,
                                     final int numResults,
                                     final String status,
                                     final List<FeedModel> items) {
            this.moreAvailable = moreAvailable;
            this.nextMaxId = nextMaxId;
            this.numResults = numResults;
            this.status = status;
            this.items = items;
        }

        public boolean isMoreAvailable() {
            return moreAvailable;
        }

        public TagPostsFetchResponse setMoreAvailable(final boolean moreAvailable) {
            this.moreAvailable = moreAvailable;
            return this;
        }

        public String getNextMaxId() {
            return nextMaxId;
        }

        public TagPostsFetchResponse setNextMaxId(final String nextMaxId) {
            this.nextMaxId = nextMaxId;
            return this;
        }

        public int getNumResults() {
            return numResults;
        }

        public TagPostsFetchResponse setNumResults(final int numResults) {
            this.numResults = numResults;
            return this;
        }

        public String getStatus() {
            return status;
        }

        public TagPostsFetchResponse setStatus(final String status) {
            this.status = status;
            return this;
        }

        public List<FeedModel> getItems() {
            return items;
        }

        public TagPostsFetchResponse setItems(final List<FeedModel> items) {
            this.items = items;
            return this;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final TagPostsFetchResponse that = (TagPostsFetchResponse) o;
            return moreAvailable == that.moreAvailable &&
                    numResults == that.numResults &&
                    Objects.equals(nextMaxId, that.nextMaxId) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(items, that.items);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moreAvailable, nextMaxId, numResults, status, items);
        }

        @NonNull
        @Override
        public String toString() {
            return "TagPostsFetchResponse{" +
                    "moreAvailable=" + moreAvailable +
                    ", nextMaxId='" + nextMaxId + '\'' +
                    ", numResults=" + numResults +
                    ", status='" + status + '\'' +
                    ", items=" + items +
                    '}';
        }
    }
}
