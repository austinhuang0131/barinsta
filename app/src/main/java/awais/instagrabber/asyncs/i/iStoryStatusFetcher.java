package awais.instagrabber.asyncs.i;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.StoryModel;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.models.stickers.PollModel;
import awais.instagrabber.models.stickers.QuestionModel;
import awais.instagrabber.models.stickers.QuizModel;
import awais.instagrabber.models.stickers.SwipeUpModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.NetworkUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class iStoryStatusFetcher extends AsyncTask<Void, Void, StoryModel[]> {
    private final String id;
    private String username;
    private final boolean isLoc;
    private final boolean isHashtag;
    private final boolean highlight;
    private final FetchListener<StoryModel[]> fetchListener;

    public iStoryStatusFetcher(final String id,
                               final String username,
                               final boolean isLoc,
                               final boolean isHashtag,
                               final boolean highlight,
                               final FetchListener<StoryModel[]> fetchListener) {
        this.id = id;
        this.username = username;
        this.isLoc = isLoc;
        this.isHashtag = isHashtag;
        this.highlight = highlight;
        this.fetchListener = fetchListener;
    }

    @Override
    protected StoryModel[] doInBackground(final Void... voids) {
        StoryModel[] result = null;
        final String userId = id.replace(":", "%3A");
        final StringBuilder builder = new StringBuilder();
        builder.append("https://i.instagram.com/api/v1/");
        if (isLoc) {
            builder.append("locations/");
        }
        if (isHashtag) {
            builder.append("tags/");
        }
        if (highlight) {
            builder.append("feed/reels_media?user_ids=");
        } else {
            builder.append("feed/user/");
        }
        builder.append(userId);
        if (!highlight) {
            builder.append("/story/");
        }
        final String url = builder.toString();
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", Constants.I_USER_AGENT);
            conn.setRequestProperty("Accept-Language", LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                JSONObject data = new JSONObject(NetworkUtils.readFromConnection(conn));
                if (!highlight)
                    data = data.optJSONObject((isLoc || isHashtag) ? "story" : "reel");
                else if (highlight) data = data.getJSONObject("reels").optJSONObject(id);

                if (username == null && !isLoc && !isHashtag)
                    username = data.getJSONObject("user").getString("username");

                JSONArray media;
                if (data != null && (media = data.optJSONArray("items")) != null
                        && media.length() > 0 && (data = media.optJSONObject(0)) != null) {

                    final int mediaLen = media.length();

                    final StoryModel[] models = new StoryModel[mediaLen];
                    for (int i = 0; i < mediaLen; ++i) {
                        data = media.getJSONObject(i);
                        final boolean isVideo = data.has("video_duration");

                        models[i] = new StoryModel(data.getString("id"),
                                data.getJSONObject("image_versions2").getJSONArray("candidates").getJSONObject(0).getString("url"),
                                isVideo ? MediaItemType.MEDIA_TYPE_VIDEO : MediaItemType.MEDIA_TYPE_IMAGE,
                                data.optLong("taken_at", 0),
                                (isLoc || isHashtag) ? data.getJSONObject("user").getString("username") : username,
                                data.getJSONObject("user").getString("pk"), data.getBoolean("can_reply"));

                        final JSONArray videoResources = data.optJSONArray("video_versions");
                        if (isVideo && videoResources != null)
                            models[i].setVideoUrl(ResponseBodyUtils.getHighQualityPost(videoResources, true, true, false));

                        if (data.has("story_feed_media")) {
                            models[i].setTappableShortCode(data.getJSONArray("story_feed_media").getJSONObject(0).optString("media_id"));
                        }

                        // assuming everything is spotify
                        if (!data.isNull("story_app_attribution"))
                            models[i].setSpotify(data.getJSONObject("story_app_attribution").optString("content_url").split("\\?")[0]);

                        if (data.has("story_polls")) {
                            JSONObject tappableObject = data.optJSONArray("story_polls").getJSONObject(0).optJSONObject("poll_sticker");
                            if (tappableObject != null) models[i].setPoll(new PollModel(
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
                            JSONObject tappableObject = data.getJSONArray("story_questions").getJSONObject(0).optJSONObject("question_sticker");
                            if (tappableObject != null && !tappableObject.getString("question_type").equals("music"))
                                models[i].setQuestion(new QuestionModel(
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
                                models[i].setQuiz(new QuizModel(
                                        String.valueOf(tappableObject.getLong("quiz_id")),
                                        tappableObject.getString("question"),
                                        choices,
                                        counts,
                                        tappableObject.optInt("viewer_answer", -1)
                                ));
                            }
                        }

                        if (data.has("story_cta") && data.has("link_text")) {
                            JSONObject tappableObject = data.getJSONArray("story_cta").getJSONObject(0).getJSONArray("links").getJSONObject(0);
                            String swipeUpUrl = tappableObject.getString("webUri");
                            if (swipeUpUrl.startsWith("http")) {
                                models[i].setSwipeUp(new SwipeUpModel(
                                        swipeUpUrl,
                                        data.getString("link_text")
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
                                        locations.getJSONObject(h).getJSONObject("location").getLong("pk")
                                                + "/ (" + locations.getJSONObject(h).getJSONObject("location").getString("short_name") + ")";
                            }
                        }
                        if (mentions.length != 0) models[i].setMentions(mentions);
                    }
                    result = models;
                }
            }

            conn.disconnect();
        } catch (final Exception e) {
            if (logCollector != null)
                logCollector.appendException(e, LogCollector.LogFile.ASYNC_STORY_STATUS_FETCHER, "doInBackground (i)");
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final StoryModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }
}