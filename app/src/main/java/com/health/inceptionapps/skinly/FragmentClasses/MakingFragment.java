package com.health.inceptionapps.skinly.FragmentClasses;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
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


public class MakingFragment extends Fragment {


    public MakingFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate( R.layout.fragment_making , container , false ) ;
        TextView textView = fragmentView.findViewById( R.id.t4 ) ;
        textView.setTextIsSelectable( true ) ;
        String s = getContext().getResources().getString(R.string.t4);
        textView.setText( Html.fromHtml( s ) );

        TitleViewModel viewModel = ViewModelProviders.of( getActivity() ).get( TitleViewModel.class ) ;
        viewModel.setTitle( getString( R.string.how_s_it_made ) ) ;

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
