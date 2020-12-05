package de.koenidv.sph.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val mText: MutableLiveData<String> = MutableLiveData()
    val text: LiveData<String>
        get() = mText

    init {
        mText.value = "This is home fragment"
    }
}