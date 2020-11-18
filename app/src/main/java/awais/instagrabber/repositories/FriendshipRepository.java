package awais.instagrabber.repositories;

import java.util.Map;

import awais.instagrabber.repositories.responses.FriendshipRepoChangeRootResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoListFetchResponse;
import awais.instagrabber.repositories.responses.FriendshipRepoRestrictRootResponse;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface FriendshipRepository {

    @FormUrlEncoded
    @POST("/api/v1/friendships/{action}/{id}/")
    Call<FriendshipRepoChangeRootResponse> change(@Header("User-Agent") String userAgent,
                                                  @Path("action") String action,
                                                  @Path("id") String id,
                                                  @FieldMap Map<String, String> form);

    @FormUrlEncoded
    @POST("/api/v1/restrict_action/{action}/")
    Call<FriendshipRepoRestrictRootResponse> toggleRestrict(@Header("User-Agent") String userAgent,
                                                            @Path("action") String action,
                                                            @FieldMap Map<String, String> form);

    @GET("/api/v1/friendships/{userId}/{type}/")
    Call<String> getList(@Header("User-Agent") String userAgent,
                         @Path("userId") String userId,
                         @Path("type") String type, // following or followers
                         @QueryMap(encoded = true) Map<String, String> queryParams);
}
