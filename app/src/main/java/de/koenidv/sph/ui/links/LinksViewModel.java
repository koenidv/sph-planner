package de.koenidv.sph.ui.links;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LinksViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public LinksViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is links fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}