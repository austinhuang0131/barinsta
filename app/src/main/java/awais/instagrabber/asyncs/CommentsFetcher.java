package awais.instagrabber.asyncs;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.Utils;
import awaisomereport.LogCollector;

import static awais.instagrabber.utils.Utils.logCollector;

public final class CommentsFetcher extends AsyncTask<Void, Void, CommentModel[]> {
    private final String shortCode;
    private final FetchListener<CommentModel[]> fetchListener;

    /*
     * i fucking spent the whole day on this and fixing all the fucking problems in this class.
     * DO NO FUCK WITH THIS CODE!
     *  -AWAiS (The Badak) @the.badak
     */
    public CommentsFetcher(final String shortCode, final FetchListener<CommentModel[]> fetchListener) {
        this.shortCode = shortCode;
        this.fetchListener = fetchListener;
    }

    @NonNull
    @Override
    protected CommentModel[] doInBackground(final Void... voids) {
         /*
        "https://www.instagram.com/graphql/query/?query_hash=97b41c52301f77ce508f55e66d17620e&variables=" + "{\"shortcode\":\"" + shortcode + "\",\"first\":50,\"after\":\"" + endCursor + "\"}";

        97b41c52301f77ce508f55e66d17620e -> for comments
        51fdd02b67508306ad4484ff574a0b62 -> for child comments

        https://www.instagram.com/graphql/query/?query_hash=51fdd02b67508306ad4484ff574a0b62&variables={"comment_id":"18100041898085322","first":50,"after":""}
         */
        final ArrayList<CommentModel> commentModels = getParentComments();

        for (final CommentModel commentModel : commentModels) {
            final CommentModel[] childCommentModels = commentModel.getChildCommentModels();
            if (childCommentModels != null) {
                final int childCommentsLen = childCommentModels.length;

                final CommentModel lastChild = childCommentModels[childCommentsLen - 1];
                if (lastChild != null && lastChild.hasNextPage() && !Utils.isEmpty(lastChild.getEndCursor())) {
                    final CommentModel[] remoteChildComments = getChildComments(commentModel.getId());
                    commentModel.setChildCommentModels(remoteChildComments);
                    lastChild.setPageCursor(false, null);
                }
            }
        }

        return commentModels.toArray(new CommentModel[0]);
    }

    @Override
    protected void onPreExecute() {
        if (fetchListener != null) fetchListener.doBefore();
    }

    @Override
    protected void onPostExecute(final CommentModel[] result) {
        if (fetchListener != null) fetchListener.onResult(result);
    }

    @NonNull
    private synchronized CommentModel[] getChildComments(final String commentId) {
        final ArrayList<CommentModel> commentModels = new ArrayList<>();

        String endCursor = "";
        while (endCursor != null) {
            final String url = "https://www.instagram.com/graphql/query/?query_hash=51fdd02b67508306ad4484ff574a0b62&variables=" +
                    "{\"comment_id\":\"" + commentId + "\",\"first\":50,\"after\":\"" + endCursor + "\"}";

            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setUseCaches(false);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) break;
                else {
                    final JSONObject data = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("data")
                            .getJSONObject("comment").getJSONObject("edge_threaded_comments");

                    final JSONObject pageInfo = data.getJSONObject("page_info");
                    endCursor = pageInfo.getString("end_cursor");
                    if (Utils.isEmpty(endCursor)) endCursor = null;

                    final JSONArray childComments = data.optJSONArray("edges");
                    if (childComments != null) {
                        final int length = childComments.length();
                        for (int i = 0; i < length; ++i) {
                            final JSONObject childComment = childComments.getJSONObject(i).optJSONObject("node");

                            if (childComment != null) {
                                final JSONObject owner = childComment.getJSONObject("owner");
                                final ProfileModel profileModel = new ProfileModel(false, false, false,
                                        owner.getString(Constants.EXTRAS_ID),
                                        owner.getString(Constants.EXTRAS_USERNAME),
                                        null, null, null,
                                        owner.getString("profile_pic_url"),
                                        null, 0, 0, 0, false, false, false, false);

                                final JSONObject likedBy = childComment.optJSONObject("edge_liked_by");

                                commentModels.add(new CommentModel(childComment.getString(Constants.EXTRAS_ID),
                                        childComment.getString("text"),
                                        childComment.getLong("created_at"),
                                        likedBy != null ? likedBy.optLong("count", 0) : 0,
                                        childComment.getBoolean("viewer_has_liked"),
                                        profileModel));
                            }
                        }
                    }
                }

                conn.disconnect();
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.ASYNC_COMMENTS_FETCHER, "getChildComments",
                            new Pair<>("commentModels.size", commentModels.size()));
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                break;
            }
        }

        return commentModels.toArray(new CommentModel[0]);
    }

    @NonNull
    private synchronized ArrayList<CommentModel> getParentComments() {
        final ArrayList<CommentModel> commentModelsList = new ArrayList<>();

        String endCursor = "";
        while (endCursor != null) {
            final String url = "https://www.instagram.com/graphql/query/?query_hash=97b41c52301f77ce508f55e66d17620e&variables=" +
                    "{\"shortcode\":\"" + shortCode + "\",\"first\":50,\"after\":\"" + endCursor + "\"}";

            try {
                final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setUseCaches(false);
                conn.connect();

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) break;
                else {
                    final JSONObject parentComments = new JSONObject(Utils.readFromConnection(conn)).getJSONObject("data")
                            .getJSONObject("shortcode_media").getJSONObject("edge_media_to_parent_comment");

                    final JSONObject pageInfo = parentComments.getJSONObject("page_info");
                    endCursor = pageInfo.optString("end_cursor");
                    if (Utils.isEmpty(endCursor)) endCursor = null;

                    // final boolean containsToken = endCursor.contains("bifilter_token");
                    // if (!Utils.isEmpty(endCursor) && (containsToken || endCursor.contains("cached_comments_cursor"))) {
                    //     final JSONObject endCursorObject = new JSONObject(endCursor);
                    //     endCursor = endCursorObject.optString("cached_comments_cursor");
                    //
                    //     if (!Utils.isEmpty(endCursor))
                    //         endCursor = "{\\\"cached_comments_cursor\\\": \\\"" + endCursor + "\\\", ";
                    //     else
                    //         endCursor = "{";
                    //
                    //     endCursor = endCursor + "\\\"bifilter_token\\\": \\\"" + endCursorObject.getString("bifilter_token") + "\\\"}";
                    // }
                    // else if (containsToken) endCursor = null;

                    final JSONArray comments = parentComments.getJSONArray("edges");
                    final int commentsLen = comments.length();
                    final CommentModel[] commentModels = new CommentModel[commentsLen];

                    for (int i = 0; i < commentsLen; ++i) {
                        final JSONObject comment = comments.getJSONObject(i).getJSONObject("node");

                        final JSONObject owner = comment.getJSONObject("owner");
                        final ProfileModel profileModel = new ProfileModel(false, false,
                                owner.optBoolean("is_verified"),
                                owner.getString(Constants.EXTRAS_ID),
                                owner.getString(Constants.EXTRAS_USERNAME),
                                null, null, null,
                                owner.getString("profile_pic_url"),
                                null, 0, 0, 0, false, false, false, false);

                        final JSONObject likedBy = comment.optJSONObject("edge_liked_by");
                        final String commentId = comment.getString(Constants.EXTRAS_ID);
                        commentModels[i] = new CommentModel(commentId,
                                comment.getString("text"),
                                comment.getLong("created_at"),
                                likedBy != null ? likedBy.optLong("count", 0) : 0,
                                comment.getBoolean("viewer_has_liked"),
                                profileModel);

                        JSONObject tempJsonObject;

                        final JSONArray childCommentsArray;
                        final int childCommentsLen;
                        if ((tempJsonObject = comment.optJSONObject("edge_threaded_comments")) != null &&
                                (childCommentsArray = tempJsonObject.optJSONArray("edges")) != null
                                && (childCommentsLen = childCommentsArray.length()) > 0) {

                            final String childEndCursor;
                            final boolean hasNextPage;
                            if ((tempJsonObject = tempJsonObject.optJSONObject("page_info")) != null) {
                                childEndCursor = tempJsonObject.optString("end_cursor");
                                hasNextPage = tempJsonObject.optBoolean("has_next_page", !Utils.isEmpty(childEndCursor));
                            } else {
                                childEndCursor = null;
                                hasNextPage = false;
                            }

                            final CommentModel[] childCommentModels = new CommentModel[childCommentsLen];
                            for (int j = 0; j < childCommentsLen; ++j) {
                                final JSONObject childComment = childCommentsArray.getJSONObject(j).getJSONObject("node");

                                tempJsonObject = childComment.getJSONObject("owner");
                                final ProfileModel childProfileModel = new ProfileModel(false, false,
                                        tempJsonObject.optBoolean("is_verified"),
                                        tempJsonObject.getString(Constants.EXTRAS_ID),
                                        tempJsonObject.getString(Constants.EXTRAS_USERNAME),
                                        null, null, null,
                                        tempJsonObject.getString("profile_pic_url"),
                                        null, 0, 0, 0, false, false, false, false);

                                tempJsonObject = childComment.optJSONObject("edge_liked_by");
                                childCommentModels[j] = new CommentModel(childComment.getString(Constants.EXTRAS_ID),
                                        childComment.getString("text"),
                                        childComment.getLong("created_at"),
                                        tempJsonObject != null ? tempJsonObject.optLong("count", 0) : 0,
                                        childComment.getBoolean("viewer_has_liked"),
                                        childProfileModel);
                            }

                            childCommentModels[childCommentsLen - 1].setPageCursor(hasNextPage, childEndCursor);

                            commentModels[i].setChildCommentModels(childCommentModels);
                        }
                    }

                    Collections.addAll(commentModelsList, commentModels);
                }

                conn.disconnect();
            } catch (final Exception e) {
                if (logCollector != null)
                    logCollector.appendException(e, LogCollector.LogFile.ASYNC_COMMENTS_FETCHER, "getParentComments",
                            new Pair<>("commentModelsList.size", commentModelsList.size()));
                if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
                break;
            }
        }

        return commentModelsList;
    }
}
