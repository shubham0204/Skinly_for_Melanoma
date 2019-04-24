package com.health.inceptionapps.skinly.FragmentClasses;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.NavigationView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.health.inceptionapps.skinly.R;

public class BottomNavFragment extends BottomSheetDialogFragment {


    public BottomNavFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View fragmentView = inflater.inflate( R.layout.fragment_bottom_nav , container , false ) ;
        Context activityContext = getContext() ;
        NavigationView navigationView = fragmentView.findViewById( R.id.bottom_navigation ) ;
        NavigationUI.setupWithNavController( navigationView , Navigation.findNavController( getActivity() ,
                R.id.nav_host_fragment));

        getDialog().getWindow().getAttributes().windowAnimations = R.style.AlertDialogAnims ;

        Button feedbackButton = fragmentView.findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND) ;
                intent.setData(Uri.parse("email"));
                intent.putExtra( Intent.EXTRA_EMAIL , new String[] { getString( R.string.dev_email ) });
                intent.putExtra( Intent.EXTRA_SUBJECT , getString(R.string.app_name) + " Feedback") ;
                intent.setType("message/rfc822");
                getContext().startActivity( Intent.createChooser( intent , "Send feedback via ..."));
            }
        });

        return fragmentView ;
    }
}
