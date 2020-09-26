package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.utils.DataBox;

public class FavoritesViewModel extends ViewModel {
    private MutableLiveData<List<DataBox.FavoriteModel>> list;

    public MutableLiveData<List<DataBox.FavoriteModel>> getList() {
        if (list == null) {
            list = new MutableLiveData<>();
        }
        return list;
    }
}
