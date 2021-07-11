package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.repositories.responses.stories.Story;

public class FeedStoriesViewModel extends ViewModel {
    private MutableLiveData<List<Story>> list;

    public MutableLiveData<List<Story>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}