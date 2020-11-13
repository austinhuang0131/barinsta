package awais.instagrabber.webservices;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.models.FeedModel;
import awais.instagrabber.models.PostChild;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.FeedRepository;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class FeedService extends BaseService {
    private static final String TAG = "FeedService";
    private static final boolean loadFromMock = false;

    private final FeedRepository repository;

    private static FeedService instance;

    private FeedService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        repository = retrofit.create(FeedRepository.class);
    }

    public static FeedService getInstance() {
        if (instance == null) {
            instance = new FeedService();
        }
        return instance;
    }

    public void fetch(final int maxItemsToLoad,
                      final String cursor,
                      final ServiceCallback<PostsFetchResponse> callback) {
        if (loadFromMock) {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                final ClassLoader classLoader = getClass().getClassLoader();
                if (classLoader == null) {
                    Log.e(TAG, "fetch: classLoader is null!");
                    return;
                }
                try (InputStream resourceAsStream = classLoader.getResourceAsStream("feed_response.json");
                     Reader in = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8)) {
                    final int bufferSize = 1024;
                    final char[] buffer = new char[bufferSize];
                    final StringBuilder out = new StringBuilder();
                    int charsRead;
                    while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
                        out.append(buffer, 0, charsRead);
                    }
                    callback.onSuccess(parseResponseBody(out.toString()));
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "fetch: ", e);
                }
            }, 1000);
            return;
        }
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "6b838488258d7a4820e48d209ef79eb1");
        queryMap.put("variables", "{" +
                "\"fetch_media_item_count\":" + maxItemsToLoad + "," +
                "\"has_threaded_comments\":true," +
                "\"fetch_media_item_cursor\":\"" + (cursor == null ? "" : cursor) + "\"" +
                "}");
        final Call<String> request = repository.fetch(queryMap);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                try {
                    // Log.d(TAG, "onResponse: body: " + response.body());
                    final PostsFetchResponse postsFetchResponse = parseResponse(response);
                    if (callback != null) {
                        callback.onSuccess(postsFetchResponse);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "onResponse", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
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

    @NonNull
    private PostsFetchResponse parseResponse(@NonNull final Response<String> response) throws JSONException {
        if (TextUtils.isEmpty(response.body())) {
            Log.e(TAG, "parseResponse: feed response body is empty with status code: " + response.code());
            return new PostsFetchResponse(Collections.emptyList(), false, null);
        }
        return parseResponseBody(response.body());
    }

    @NonNull
    private PostsFetchResponse parseResponseBody(@NonNull final String body)
            throws JSONException {
        final List<FeedModel> feedModels = new ArrayList<>();
        final JSONObject timelineFeed = new JSONObject(body)
                .getJSONObject("data")
                .getJSONObject(Constants.EXTRAS_USER)
                .getJSONObject("edge_web_feed_timeline");
        final String endCursor;
        final boolean hasNextPage;

        final JSONObject pageInfo = timelineFeed.getJSONObject("page_info");
        if (pageInfo.has("has_next_page")) {
            hasNextPage = pageInfo.getBoolean("has_next_page");
            endCursor = hasNextPage ? pageInfo.getString("end_cursor") : null;
        } else {
            hasNextPage = false;
            endCursor = null;
        }

        final JSONArray feedItems = timelineFeed.getJSONArray("edges");

        for (int i = 0; i < feedItems.length(); ++i) {
            final JSONObject itemJson = feedItems.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            final FeedModel feedModel = ResponseBodyUtils.parseItem(itemJson);
            feedModels.add(feedModel);
        }
        return new PostsFetchResponse(feedModels, hasNextPage, endCursor);
    }
}
