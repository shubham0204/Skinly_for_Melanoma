package com.health.inceptionapps.skinly.FragmentClasses;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.health.inceptionapps.skinly.R;
import com.health.inceptionapps.skinly.ViewModelClasses.TitleViewModel;

public class SymptomsFragment extends Fragment {


    public SymptomsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate( R.layout.fragment_symptoms , container , false ) ;

        TextView textView = fragmentView.findViewById( R.id.t2 ) ;
        textView.setTextIsSelectable( true ) ;
        String s = getContext().getResources().getString(R.string.t2);
        textView.setText( Html.fromHtml( s ) );

        TitleViewModel viewModel = ViewModelProviders.of( getActivity() ).get( TitleViewModel.class ) ;
        viewModel.setTitle( getString( R.string.symptoms ) ) ;

        Slide slide = new Slide() ;
        slide.setSlideEdge( Gravity.BOTTOM ) ;
        slide.setInterpolator( new DecelerateInterpolator() ) ;
        slide.setDuration( getResources().getInteger( R.integer.app_fragment_enter_duration )  ) ;

        Fade fade = new Fade() ;
        fade.setDuration( getResources().getInteger( R.integer.app_fragment_enter_duration ) ) ;

        TransitionSet set = new TransitionSet() ;
        set.addTransition( slide ) ;
        set.addTransition( fade ) ;

        setEnterTransition( set ) ;

        return fragmentView ;
    }

}
