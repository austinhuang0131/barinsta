package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.repositories.responses.stories.StoryMedia;

public class StoriesViewModel extends ViewModel {
    private MutableLiveData<List<StoryMedia>> list;

    public MutableLiveData<List<StoryMedia>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}