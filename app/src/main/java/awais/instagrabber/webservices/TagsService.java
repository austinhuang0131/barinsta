package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import awais.instagrabber.repositories.TagsRepository;
import awais.instagrabber.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class TagsService extends BaseService {

    private static final String TAG = "TagsService";

    // web for www.instagram.com
    private final TagsRepository webRepository;

    private static TagsService instance;

    private TagsService() {
        final Retrofit webRetrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com/")
                .build();
        webRepository = webRetrofit.create(TagsRepository.class);
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
}
