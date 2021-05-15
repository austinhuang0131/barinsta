package awais.instagrabber.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.FeedStoryModel;

public class FeedStoriesViewModel extends ViewModel implements StoriesViewModel<FeedStoryModel> {
    private final MutableLiveData<List<FeedStoryModel>> list = new MutableLiveData<>();

    public LiveData<List<FeedStoryModel>> getList() {
        return list;
    }

    @Override
    public void setList(final List<FeedStoryModel> result) {
        list.postValue(result);
    }
}