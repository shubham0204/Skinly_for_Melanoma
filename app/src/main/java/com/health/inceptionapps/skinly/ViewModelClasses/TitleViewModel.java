package com.health.inceptionapps.skinly.ViewModelClasses;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class TitleViewModel extends ViewModel {

    private final MutableLiveData<String> title = new MutableLiveData<>() ;

    public MutableLiveData<String> getTitle() {
        return title;
    }

    public void setTitle( String title )  {
        this.title.setValue( title ) ;
    }

}
