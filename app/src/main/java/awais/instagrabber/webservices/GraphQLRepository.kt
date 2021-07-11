package awais.instagrabber.webservices

import android.util.Log
import awais.instagrabber.models.enums.FollowingType
import awais.instagrabber.repositories.GraphQLService
import awais.instagrabber.repositories.responses.*
import awais.instagrabber.utils.Constants
import awais.instagrabber.utils.ResponseBodyUtils
import awais.instagrabber.utils.extensions.TAG
import org.json.JSONException
import org.json.JSONObject
import java.util.*


open class GraphQLRepository(private val service: GraphQLService) {

    // TODO convert string response to a response class
    private suspend fun fetch(
        queryHash: String,
        variables: String,
        arg1: String,
        arg2: String,
        backup: User?,
    ): PostsFetchResponse {
        val queryMap = mapOf(
            "query_hash" to queryHash,
            "variables" to variables,
        )
        val response = service.fetch(queryMap)
        return parsePostResponse(response, arg1, arg2, backup)
    }

    suspend fun fetchLocationPosts(
        locationId: Long,
        maxId: String?,
    ): PostsFetchResponse = fetch(
        "36bd0f2bf5911908de389b8ceaa3be6d",
        "{\"id\":\"" + locationId + "\"," + "\"first\":25," + "\"after\":\"" + (maxId ?: "") + "\"}",
        Constants.EXTRAS_LOCATION,
        "edge_location_to_media",
        null
    )

    suspend fun fetchHashtagPosts(
        tag: String,
        maxId: String?,
    ): PostsFetchResponse = fetch(
        "9b498c08113f1e09617a1703c22b2f32",
        "{\"tag_name\":\"" + tag + "\"," + "\"first\":25," + "\"after\":\"" + (maxId ?: "") + "\"}",
        Constants.EXTRAS_HASHTAG,
        "edge_hashtag_to_media",
        null,
    )

    suspend fun fetchProfilePosts(
        profileId: Long,
        postsPerPage: Int,
        maxId: String?,
        backup: User?,
    ): PostsFetchResponse = fetch(
        "02e14f6a7812a876f7d133c9555b1151",
        "{\"id\":\"" + profileId + "\"," + "\"first\":" + postsPerPage + "," + "\"after\":\"" + (maxId ?: "") + "\"}",
        Constants.EXTRAS_USER,
        "edge_owner_to_timeline_media",
        backup,
    )

    suspend fun fetchTaggedPosts(
        profileId: Long,
        postsPerPage: Int,
        maxId: String?,
    ): PostsFetchResponse = fetch(
        "31fe64d9463cbbe58319dced405c6206",
        "{\"id\":\"" + profileId + "\"," + "\"first\":" + postsPerPage + "," + "\"after\":\"" + (maxId ?: "") + "\"}",
        Constants.EXTRAS_USER,
        "edge_user_to_photos_of_you",
        null,
    )

    @Throws(JSONException::class)
    private fun parsePostResponse(
        response: String,
        arg1: String,
        arg2: String,
        backup: User?,
    ): PostsFetchResponse {
        if (response.isBlank()) {
            Log.e(TAG, "parseResponse: feed response body is empty")
            return PostsFetchResponse(emptyList(), false, null)
        }
        return parseResponseBody(response, arg1, arg2, backup)
    }

    @Throws(JSONException::class)
    private fun parseResponseBody(
        body: String,
        arg1: String,
        arg2: String,
        backup: User?,
    ): PostsFetchResponse {
        val items: MutableList<Media> = ArrayList()
        val timelineFeed = JSONObject(body)
            .getJSONObject("data")
            .getJSONObject(arg1)
            .getJSONObject(arg2)
        val endCursor: String?
        val hasNextPage: Boolean
        val pageInfo = timelineFeed.getJSONObject("page_info")
        if (pageInfo.has("has_next_page")) {
            hasNextPage = pageInfo.getBoolean("has_next_page")
            endCursor = if (hasNextPage) pageInfo.getString("end_cursor") else null
        } else {
            hasNextPage = false
            endCursor = null
        }
        val feedItems = timelineFeed.getJSONArray("edges")
        for (i in 0 until feedItems.length()) {
            val itemJson = feedItems.optJSONObject(i) ?: continue
            val media = ResponseBodyUtils.parseGraphQLItem(itemJson, backup)
            if (media != null) {
                items.add(media)
            }
        }
        return PostsFetchResponse(items, hasNextPage, endCursor)
    }

    // TODO convert string response to a response class
    suspend fun fetchCommentLikers(
        commentId: String,
        endCursor: String?,
    ): GraphQLUserListFetchResponse {
        val queryMap = mapOf(
            "query_hash" to "5f0b1f6281e72053cbc07909c8d154ae",
            "variables" to "{\"comment_id\":\"" + commentId + "\"," + "\"first\":30," + "\"after\":\"" + (endCursor ?: "") + "\"}"
        )
        val response = service.fetch(queryMap)
        val body = JSONObject(response)
        val status = body.getString("status")
        val data = body.getJSONObject("data").getJSONObject("comment").getJSONObject("edge_liked_by")
        val pageInfo = data.getJSONObject("page_info")
        val newEndCursor = if (pageInfo.getBoolean("has_next_page")) pageInfo.getString("end_cursor") else null
        val users = data.getJSONArray("edges")
        val usersLen = users.length()
        val userModels: MutableList<User> = ArrayList()
        for (j in 0 until usersLen) {
            val userObject = users.getJSONObject(j).getJSONObject("node")
            userModels.add(
                User(
                    userObject.getLong("id"),
                    userObject.getString("username"),
                    userObject.optString("full_name"),
                    userObject.optBoolean("is_private"),
                    userObject.getString("profile_pic_url"),
                    userObject.optBoolean("is_verified")
                )
            )
        }
        return GraphQLUserListFetchResponse(newEndCursor, status, userModels)
    }

    suspend fun fetchComments(
        shortCodeOrCommentId: String?,
        root: Boolean,
        cursor: String?,
    ): String {
        val variables = mapOf(
            (if (root) "shortcode" else "comment_id") to shortCodeOrCommentId,
            "first" to 50,
            "after" to (cursor ?: "")
        )
        val queryMap = mapOf(
            "query_hash" to if (root) "bc3296d1ce80a24b1b6e40b1e72903f5" else "51fdd02b67508306ad4484ff574a0b62",
            "variables" to JSONObject(variables).toString()
        )
        return service.fetch(queryMap)
    }

    // TODO convert string response to a response class
    open suspend fun fetchUser(
        username: String,
    ): User {
        val response = service.getUser(username)
        val body = JSONObject(response
            .split("<script type=\"text/javascript\">window._sharedData = ").get(1)
            .split("</script>").get(0)
            .trim().replace(Regex("\\};$"), "}"))
        val userJson = body
            .getJSONObject("entry_data")
            .getJSONArray("ProfilePage")
            .getJSONObject(0)
            .getJSONObject("graphql")
            .getJSONObject(Constants.EXTRAS_USER)
        val isPrivate = userJson.getBoolean("is_private")
        val id = userJson.optLong(Constants.EXTRAS_ID, 0)
        val timelineMedia = userJson.getJSONObject("edge_owner_to_timeline_media")
        // if (timelineMedia.has("edges")) {
        //     final JSONArray edges = timelineMedia.getJSONArray("edges");
        // }
        var url: String? = userJson.optString("external_url")
        if (url.isNullOrBlank()) url = null
        return User(
            id,
            username,
            userJson.getString("full_name"),
            isPrivate,
            userJson.getString("profile_pic_url_hd"),
            userJson.getBoolean("is_verified"),
            friendshipStatus = FriendshipStatus(
                userJson.optBoolean("followed_by_viewer"),
                userJson.optBoolean("follows_viewer"),
                userJson.optBoolean("blocked_by_viewer"),
                false,
                isPrivate,
                userJson.optBoolean("has_requested_viewer"),
                userJson.optBoolean("requested_by_viewer"),
                false,
                userJson.optBoolean("restricted_by_viewer"),
                false
            ),
            mediaCount = timelineMedia.getLong("count"),
            followerCount = userJson.getJSONObject("edge_followed_by").getLong("count"),
            followingCount = userJson.getJSONObject("edge_follow").getLong("count"),
            biography = userJson.getString("biography"),
            externalUrl = url,
        )
    }

    // TODO convert string response to a response class
    suspend fun fetchPost(
        shortcode: String,
    ): Media {
        val response = service.getPost(shortcode)
        val body = JSONObject(response)
        val media = body.getJSONObject("graphql").getJSONObject("shortcode_media")
        return ResponseBodyUtils.parseGraphQLItem(media, null)
    }

    // TODO convert string response to a response class
    suspend fun fetchTag(
        tag: String,
    ): Hashtag {
        val response = service.getTag(tag)
        val body = JSONObject(response
            .split("<script type=\"text/javascript\">window._sharedData = ").get(1)
            .split("</script>").get(0)
            .trim().replace(Regex("\\};$"), "}"))
            .getJSONObject("entry_data")
            .getJSONArray("TagPage")
            .getJSONObject(0)
            .getJSONObject("graphql")
            .getJSONObject(Constants.EXTRAS_HASHTAG)
        val timelineMedia = body.getJSONObject("edge_hashtag_to_media")
        return Hashtag(
            body.getString(Constants.EXTRAS_ID),
            body.getString("name"),
            timelineMedia.getLong("count"),
            if (body.optBoolean("is_following")) FollowingType.FOLLOWING else FollowingType.NOT_FOLLOWING,
            null
        )
    }

    // TODO convert string response to a response class
    suspend fun fetchLocation(
        locationId: Long,
    ): Location {
        val response = service.getLocation(locationId)
        val body = JSONObject(response
            .split("<script type=\"text/javascript\">window._sharedData = ", "</script>").get(1)
            .trim().replace(Regex("};$"), "}"))
            .getJSONObject("entry_data")
            .getJSONArray("LocationsPage")
            .getJSONObject(0)
            .getJSONObject("graphql")
            .getJSONObject(Constants.EXTRAS_LOCATION)
        // val timelineMedia = body.getJSONObject("edge_location_to_media")
        val address = JSONObject(body.getString("address_json"))
        return Location(
            body.getLong(Constants.EXTRAS_ID),
            body.getString("slug"),
            body.getString("name"),
            address.optString("street_address"),
            address.optString("city_name"),
            body.optDouble("lng", 0.0),
            body.optDouble("lat", 0.0)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: GraphQLRepository? = null

        fun getInstance(): GraphQLRepository {
            return INSTANCE ?: synchronized(this) {
                val service: GraphQLService = RetrofitFactory.retrofitWeb.create(GraphQLService::class.java)
                GraphQLRepository(service).also { INSTANCE = it }
            }
        }
    }
}