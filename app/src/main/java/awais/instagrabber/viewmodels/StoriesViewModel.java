package awais.instagrabber.viewmodels;

import androidx.lifecycle.LiveData;

import java.util.List;

public interface StoriesViewModel<T> {
    LiveData<List<T>> getList();

    void setList(List<T> result);
}
