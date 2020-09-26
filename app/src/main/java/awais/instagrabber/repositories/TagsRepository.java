package awais.instagrabber.repositories;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TagsRepository {

    @POST("/web/tags/follow/{tag}/")
    Call<String> follow(@Header("User-Agent") String userAgent,
                        @Header("x-csrftoken") String csrfToken,
                        @Path("tag") String tag);

    @POST("/web/tags/unfollow/{tag}/")
    Call<String> unfollow(@Header("User-Agent") String userAgent,
                          @Header("x-csrftoken") String csrfToken,
                          @Path("tag") String tag);
}
