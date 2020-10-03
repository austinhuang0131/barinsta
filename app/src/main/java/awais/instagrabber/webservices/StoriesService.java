package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.models.FeedStoryModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.repositories.StoriesRepository;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.ResponseBodyUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StoriesService extends BaseService {
    private static final String TAG = "StoriesService";

    private final StoriesRepository repository;

    private static StoriesService instance;

    private StoriesService() {
        final Retrofit retrofit = getRetrofitBuilder()
                .baseUrl("https://www.instagram.com")
                .build();
        repository = retrofit.create(StoriesRepository.class);
    }

    public static StoriesService getInstance() {
        if (instance == null) {
            instance = new StoriesService();
        }
        return instance;
    }

    public void getFeedStories(final ServiceCallback<List<FeedStoryModel>> callback) {
        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put("query_hash", "b7b84d884400bc5aa7cfe12ae843a091");
        queryMap.put("variables", "{\"only_stories\":true,\"stories_prefetch\":false,\"stories_video_dash_manifest\":false}");
        final Call<String> response = repository.getStories(queryMap);
        response.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                final String body = response.body();
                if (body == null) {
                    Log.e(TAG, "getFeedStories: body is empty");
                    return;
                }
                try {
                    final List<FeedStoryModel> feedStoryModels = new ArrayList<>();
                    final JSONArray feedStoriesReel = new JSONObject(body)
                            .getJSONObject("data")
                            .getJSONObject(Constants.EXTRAS_USER)
                            .getJSONObject("feed_reels_tray")
                            .getJSONObject("edge_reels_tray_to_reel")
                            .getJSONArray("edges");
                    for (int i = 0; i < feedStoriesReel.length(); ++i) {
                        final JSONObject node = feedStoriesReel.getJSONObject(i).getJSONObject("node");
                        final JSONObject user = node.getJSONObject(node.has("user") ? "user" : "owner");
                        final ProfileModel profileModel = new ProfileModel(false, false, false,
                                user.getString("id"),
                                user.getString("username"),
                                null, null, null,
                                user.getString("profile_pic_url"),
                                null, 0, 0, 0, false, false, false, false);
                        final String id = node.getString("id");
                        final boolean fullyRead = !node.isNull("seen") && node.getLong("seen") == node.getLong("latest_reel_media");
                        feedStoryModels.add(new FeedStoryModel(id, profileModel, fullyRead));
                    }
                    callback.onSuccess(feedStoryModels);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json", e);
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void getUserStory(final String id,
                             final String username,
                             final boolean isLoc,
                             final boolean isHashtag,
                             final boolean highlight,
                             final ServiceCallback<List<StoryModel>> callback) {
        final String url = buildUrl(id, isLoc, isHashtag, highlight);
        final Call<String> userStoryCall = repository.getUserStory(Constants.I_USER_AGENT, url);
        userStoryCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull final Call<String> call, @NonNull final Response<String> response) {
                JSONObject data;
                String localUsername = username;
                try {
                    final String body = response.body();
                    if (body == null) {
                        Log.e(TAG, "body is null");
                        return;
                    }
                    data = new JSONObject(body);

                    if (!highlight)
                        data = data.optJSONObject((isLoc || isHashtag) ? "story" : "reel");
                    else if (highlight) data = data.getJSONObject("reels").optJSONObject(id);

                    if (data != null
                            && localUsername == null
                            && !isLoc
                            && !isHashtag)
                        localUsername = data.getJSONObject("user").getString("username");

                    JSONArray media;
                    if (data != null
                            && (media = data.optJSONArray("items")) != null
                            && media.length() > 0 && media.optJSONObject(0) != null) {

                        final int mediaLen = media.length();
                        final List<StoryModel> models = new ArrayList<>();
                        for (int i = 0; i < mediaLen; ++i) {
                            data = media.getJSONObject(i);
                            final boolean isVideo = data.has("video_duration");
                            final StoryModel model = new StoryModel(data.getString("id"),
                                    data.getJSONObject("image_versions2").getJSONArray("candidates").getJSONObject(0).getString("url"),
                                    isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                    data.optLong("taken_at", 0),
                                    (isLoc || isHashtag) ? data.getJSONObject("user").getString("username") : localUsername,
                                    data.getJSONObject("user").getString("pk"),
                                    data.getBoolean("can_reply"));

                            final JSONArray videoResources = data.optJSONArray("video_versions");
                            if (isVideo && videoResources != null)
                                model.setVideoUrl(ResponseBodyUtils.getHighQualityPost(videoResources, true, true, false));

                            if (data.has("story_feed_media")) {
                                model.setTappableShortCode(data.getJSONArray("story_feed_media").getJSONObject(0).optString("media_id"));
                            }

                            // TODO: this may not be limited to spotify
                            if (!data.isNull("story_app_attribution"))
                                model.setSpotify(data.getJSONObject("story_app_attribution").optString("content_url").split("\\?")[0]);

                            if (data.has("story_polls")) {
                                final JSONArray storyPolls = data.optJSONArray("story_polls");
                                JSONObject tappableObject = null;
                                if (storyPolls != null) {
                                    tappableObject = storyPolls.getJSONObject(0).optJSONObject("poll_sticker");
                                }
                                if (tappableObject != null) model.setPoll(new PollModel(
                                        String.valueOf(tappableObject.getLong("poll_id")),
                                        tappableObject.getString("question"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(0).getString("text"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(0).getInt("count"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(1).getString("text"),
                                        tappableObject.getJSONArray("tallies").getJSONObject(1).getInt("count"),
                                        tappableObject.optInt("viewer_vote", -1)
                                ));
                            }
                            if (data.has("story_questions")) {
                                final JSONObject tappableObject = data.getJSONArray("story_questions").getJSONObject(0).optJSONObject("question_sticker");
                                if (tappableObject != null && !tappableObject.getString("question_type").equals("music"))
                                    model.setQuestion(new QuestionModel(
                                            String.valueOf(tappableObject.getLong("question_id")),
                                            tappableObject.getString("question")
                                    ));
                            }
                            if (data.has("story_quizs")) {
                                JSONObject tappableObject = data.getJSONArray("story_quizs").getJSONObject(0).optJSONObject("quiz_sticker");
                                if (tappableObject != null) {
                                    String[] choices = new String[tappableObject.getJSONArray("tallies").length()];
                                    Long[] counts = new Long[choices.length];
                                    for (int q = 0; q < choices.length; ++q) {
                                        JSONObject tempchoice = tappableObject.getJSONArray("tallies").getJSONObject(q);
                                        choices[q] = (q == tappableObject.getInt("correct_answer") ? "*** " : "")
                                                + tempchoice.getString("text");
                                        counts[q] = tempchoice.getLong("count");
                                    }
                                    model.setQuiz(new QuizModel(
                                            String.valueOf(tappableObject.getLong("quiz_id")),
                                            tappableObject.getString("question"),
                                            choices,
                                            counts,
                                            tappableObject.optInt("viewer_answer", -1)
                                    ));
                                }
                            }
                            JSONArray hashtags = data.optJSONArray("story_hashtags");
                            JSONArray locations = data.optJSONArray("story_locations");
                            JSONArray atmarks = data.optJSONArray("reel_mentions");
                            String[] mentions = new String[(hashtags == null ? 0 : hashtags.length())
                                    + (atmarks == null ? 0 : atmarks.length())
                                    + (locations == null ? 0 : locations.length())];
                            if (hashtags != null) {
                                for (int h = 0; h < hashtags.length(); ++h) {
                                    mentions[h] = "#" + hashtags.getJSONObject(h).getJSONObject("hashtag").getString("name");
                                }
                            }
                            if (atmarks != null) {
                                for (int h = 0; h < atmarks.length(); ++h) {
                                    mentions[h + (hashtags == null ? 0 : hashtags.length())] =
                                            "@" + atmarks.getJSONObject(h).getJSONObject("user").getString("username");
                                }
                            }
                            if (locations != null) {
                                for (int h = 0; h < locations.length(); ++h) {
                                    mentions[h + (hashtags == null ? 0 : hashtags.length()) + (atmarks == null ? 0 : atmarks.length())] =
                                            locations.getJSONObject(h).getJSONObject("location").getString("short_name")
                                                    + " (" + locations.getJSONObject(h).getJSONObject("location").getLong("pk") + ")";
                                }
                            }
                            if (mentions.length != 0) model.setMentions(mentions);
                            models.add(model);
                        }
                        callback.onSuccess(models);
                    }
                    else {
                        callback.onSuccess(null);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing string");
                }
            }

            @Override
            public void onFailure(@NonNull final Call<String> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    private String buildUrl(final String id, final boolean isLoc, final boolean isHashtag, final boolean highlight) {
        final String userId = id.replace(":", "%3A");
        final StringBuilder builder = new StringBuilder();
        builder.append("https://i.instagram.com/api/v1/");
        if (isLoc) {
            builder.append("locations/");
        }
        else if (isHashtag) {
            builder.append("tags/");
        }
        else if (highlight) {
            builder.append("feed/reels_media/?user_ids=");
        }
        else {
            builder.append("feed/user/");
        }
        builder.append(userId);
        if (!highlight) {
            builder.append("/story/");
        }
        return builder.toString();
    }
}
