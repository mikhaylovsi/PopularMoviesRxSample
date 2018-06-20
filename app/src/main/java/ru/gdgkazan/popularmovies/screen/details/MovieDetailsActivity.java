package ru.gdgkazan.popularmovies.screen.details;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import ru.gdgkazan.popularmovies.R;
import ru.gdgkazan.popularmovies.model.content.Movie;
import ru.gdgkazan.popularmovies.model.content.Review;
import ru.gdgkazan.popularmovies.model.content.Video;
import ru.gdgkazan.popularmovies.model.response.ReviewsResponse;
import ru.gdgkazan.popularmovies.model.response.VideosResponse;
import ru.gdgkazan.popularmovies.network.ApiFactory;
import ru.gdgkazan.popularmovies.network.MovieService;
import ru.gdgkazan.popularmovies.screen.loading.LoadingDialog;
import ru.gdgkazan.popularmovies.screen.loading.LoadingView;
import ru.gdgkazan.popularmovies.utils.Images;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String MAXIMUM_RATING = "10";

    public static final String IMAGE = "image";
    public static final String EXTRA_MOVIE = "extraMovie";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbar;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    TextView mTitleTextView;

    @BindView(R.id.overview)
    TextView mOverviewTextView;

    @BindView(R.id.rating)
    TextView mRatingTextView;


    @BindView(R.id.reviews)
    TextView mReviewsTextView;

    @BindView(R.id.trailers)
    TextView mTrailersTextView;

    private CompositeSubscription compositeSubscription;
    private ArrayList<Video> trailers = new ArrayList<>();
    private ArrayList<Review> movieReviews = new ArrayList<>();

    public static void navigate(@NonNull AppCompatActivity activity, @NonNull View transitionImage,
                                @NonNull Movie movie) {
        Intent intent = new Intent(activity, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, IMAGE);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareWindowForAnimation();
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        compositeSubscription = new CompositeSubscription();

        ViewCompat.setTransitionName(findViewById(R.id.app_bar), IMAGE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Movie movie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        showMovie(movie);

        if(savedInstanceState != null){
            movieReviews = savedInstanceState.getParcelableArrayList("reviews");
            trailers = savedInstanceState.getParcelableArrayList("trailers");
            showTrailers(trailers);
            showReviews(movieReviews);
        } else {

            LoadingView loadingView = LoadingDialog.view(getSupportFragmentManager());


            MovieService movieService = ApiFactory.getMoviesService();
            Observable<List<Review>> reviewsObservable = movieService.reviews(String.valueOf(movie.getId()))
                    .map(ReviewsResponse::getReviews)
                    .flatMap((Func1<List<Review>, Observable<List<Review>>>) reviews -> {

                        movieReviews.clear();
                        movieReviews.addAll(reviews);

                        Realm.getDefaultInstance().executeTransaction(realm -> {
                            realm.delete(Review.class);
                            realm.insert(reviews);
                        });

                        return Observable.just(reviews);
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Func1<List<Review>, Observable<List<Review>>>) reviews -> {
                        showReviews(reviews);
                        return Observable.just(reviews);
                    })
                    .subscribeOn(Schedulers.io())
                    .onErrorResumeNext(throwable -> {
                        List<Review> reviews = Realm.getDefaultInstance().where(Review.class).findAll();
                        return Observable.just(Realm.getDefaultInstance().copyFromRealm(reviews));
                    });

            Observable<List<Video>> videosResponseObservable = movieService.trailers(String.valueOf(movie.getId()))
                    .map(VideosResponse::getVideos)
                    .flatMap((Func1<List<Video>, Observable<List<Video>>>) videos -> {
                        trailers.addAll(videos);
                        Realm.getDefaultInstance().executeTransaction(realm -> {
                            realm.delete(Video.class);
                            realm.insert(videos);
                        });

                        return Observable.just(videos);
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .flatMap((Func1<List<Video>, Observable<List<Video>>>) videos -> {
                        showTrailers(videos);
                        return Observable.just(videos);
                    })
                    .onErrorResumeNext(throwable -> {
                        List<Video> videos = Realm.getDefaultInstance().where(Video.class).findAll();
                        return Observable.just(Realm.getDefaultInstance().copyFromRealm(videos));
                    });


            reviewsObservable.zipWith(videosResponseObservable, new Func2<List<Review>, List<Video>, Boolean>() {

                @Override
                public Boolean call(List<Review> reviews, List<Video> videos) {
                    return true;
                }
            })
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(loadingView::showLoadingIndicator)
                    .doAfterTerminate(loadingView::hideLoadingIndicator)
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {

                        }
                    });

        }



        /**
         * TODO : task
         *
         * Load movie trailers and reviews and display them
         *
         * 1) See http://docs.themoviedb.apiary.io/#reference/movies/movieidtranslations/get?console=1
         * http://docs.themoviedb.apiary.io/#reference/movies/movieidtranslations/get?console=1
         * for API documentation
         *
         * 2) Add requests to {@link ru.gdgkazan.popularmovies.network.MovieService} for trailers and videos
         *
         * 3) Execute requests in parallel and show loading progress until both of them are finished
         *
         * 4) Save trailers and videos to Realm and use cached version when error occurred
         *
         * 5) Handle lifecycle changes any way you like
         */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareWindowForAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(compositeSubscription != null){
            compositeSubscription.unsubscribe();
        }
    }

    private void showMovie(@NonNull Movie movie) {
        String title = getString(R.string.movie_details);
        mCollapsingToolbar.setTitle(title);
        mCollapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, android.R.color.transparent));

        Images.loadMovie(mImage, movie, Images.WIDTH_780);

        String year = movie.getReleasedDate().substring(0, 4);
        mTitleTextView.setText(getString(R.string.movie_title, movie.getTitle(), year));
        mOverviewTextView.setText(movie.getOverview());

        String average = String.valueOf(movie.getVoteAverage());
        average = average.length() > 3 ? average.substring(0, 3) : average;
        average = average.length() == 3 && average.charAt(2) == '0' ? average.substring(0, 1) : average;
        mRatingTextView.setText(getString(R.string.rating, average, MAXIMUM_RATING));
    }

    private void showTrailers(@NonNull List<Video> videos) {

        mTrailersTextView.setText("");
        mTrailersTextView.append("Trailers" + "\n");

        for (Video x : videos){

            mTrailersTextView.append(x.getName() + "\n");

        }
    }

    private void showReviews(@NonNull List<Review> reviews) {

        mReviewsTextView.setText("");
        mReviewsTextView.append("Reviews" + "\n\n\n");

        for (Review x : reviews){

            mReviewsTextView.append(x.getAuthor() + "\n\n");
            mReviewsTextView.append(x.getContent() + "\n\n\n");

        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("trailers", trailers);
        outState.putParcelableArrayList("reviews", movieReviews);
    }

}
