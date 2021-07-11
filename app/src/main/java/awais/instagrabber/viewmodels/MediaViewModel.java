package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.KeywordsFilterUtilsKt;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class MediaViewModel extends ViewModel {
    private static final String TAG = MediaViewModel.class.getSimpleName();

    private boolean refresh = true;

    private final PostFetcher postFetcher;
    private final MutableLiveData<List<Media>> list = new MutableLiveData<>();

    public MediaViewModel(@NonNull final PostFetcher.PostFetchService postFetchService) {
        final FetchListener<List<Media>> fetchListener = new FetchListener<List<Media>>() {
            @Override
            public void onResult(final List<Media> result) {
                if (refresh) {
                    list.postValue(filterResult(result, true));
                    refresh = false;
                    return;
                }
                list.postValue(filterResult(result, false));
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        };
        postFetcher = new PostFetcher(postFetchService, fetchListener);
    }

    @NonNull
    private List<Media> filterResult(final List<Media> result, final boolean isRefresh) {
        final List<Media> models = list.getValue();
        final List<Media> modelsCopy = models == null || isRefresh ? new ArrayList<>() : new ArrayList<>(models);
        if (settingsHelper.getBoolean(PreferenceKeys.TOGGLE_KEYWORD_FILTER)) {
            final List<String> keywords = new ArrayList<>(settingsHelper.getStringSet(PreferenceKeys.KEYWORD_FILTERS));
            final List<Media> filter = KeywordsFilterUtilsKt.filter(keywords, result);
            if (filter != null) {
                modelsCopy.addAll(filter);
            }
            return modelsCopy;
        }
        modelsCopy.addAll(result);
        return modelsCopy;
    }

    public LiveData<List<Media>> getList() {
        return list;
    }

    public boolean hasMore() {
        return postFetcher.hasMore();
    }

    public void fetch() {
        postFetcher.fetch();
    }

    public void reset() {
        postFetcher.reset();
    }

    public boolean isFetching() {
        return postFetcher.isFetching();
    }

    public void refresh() {
        refresh = true;
        reset();
        fetch();
    }

    public static class ViewModelFactory implements ViewModelProvider.Factory {

        @NonNull
        private final PostFetcher.PostFetchService postFetchService;

        public ViewModelFactory(@NonNull final PostFetcher.PostFetchService postFetchService) {
            this.postFetchService = postFetchService;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull final Class<T> modelClass) {
            //noinspection unchecked
            return (T) new MediaViewModel(postFetchService);
        }
    }
}