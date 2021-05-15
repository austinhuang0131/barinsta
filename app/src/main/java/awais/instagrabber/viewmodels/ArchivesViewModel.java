package awais.instagrabber.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.models.HighlightModel;

public class ArchivesViewModel extends ViewModel implements StoriesViewModel<HighlightModel> {
    private final MutableLiveData<List<HighlightModel>> list = new MutableLiveData<>();

    public LiveData<List<HighlightModel>> getList() {
        return list;
    }

    @Override
    public void setList(final List<HighlightModel> result) {
        list.postValue(result);
    }
}