package ru.gdgkazan.popularmovies.network;

import retrofit2.http.GET;
import retrofit2.http.Path;
import ru.gdgkazan.popularmovies.model.content.Video;
import ru.gdgkazan.popularmovies.model.response.MoviesResponse;
import ru.gdgkazan.popularmovies.model.response.ReviewsResponse;
import ru.gdgkazan.popularmovies.model.response.VideosResponse;
import rx.Observable;

/**
 * @author Artur Vasilov
 */
public interface MovieService {

    @GET("popular/")
    Observable<MoviesResponse> popularMovies();

    @GET("{movieId}/videos")
    Observable<VideosResponse> trailers(@Path("movieId") String movieId);

    @GET("{movieId}/reviews")
    Observable<ReviewsResponse> reviews(@Path("movieId") String movieId);

}
