package com.app.egh.tripplanner.fragments;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.egh.tripplanner.R;
import com.app.egh.tripplanner.activities.AlarmActivity;
import com.app.egh.tripplanner.activities.DetailedActivity;
import com.app.egh.tripplanner.activities.EditTripActivity;
import com.app.egh.tripplanner.data.model.Adapter;
import com.app.egh.tripplanner.data.model.NoteHeadService;
import com.app.egh.tripplanner.data.model.Trip;

import java.util.Date;
import java.util.Locale;

import static com.app.egh.tripplanner.data.model.Adapter.isPast;

/**
 * A simple {@link Fragment} subclass.
 */
public class DetailedFragment extends Fragment {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private static final String TAG = "DetailedFragment";

    TextView trip_name;
    TextView trip_from;
    TextView trip_to;
    TextView trip_date_time;
    ImageView repeated;
    ImageView roundTrip;
    Button editTrip;
    Button startTrip;
    LinearLayout notes;

    Trip trip;

    public DetailedFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_detailed, container, false);

        trip_name = view.findViewById(R.id.trip_name);
        trip_from = view.findViewById(R.id.trip_from);
        trip_to = view.findViewById(R.id.trip_to);
        trip_date_time = view.findViewById(R.id.trip_date_time);
        repeated = view.findViewById(R.id.trip_repeated);
        roundTrip = view.findViewById(R.id.trip_roundTrip);
        notes = view.findViewById(R.id.trip_notes);
        editTrip = view.findViewById(R.id.editTrip);
        startTrip = view.findViewById(R.id.startTripFromDetails);
        Intent intent = getActivity().getIntent();
        trip = (Trip) intent.getSerializableExtra("trip");

        if(trip.isStarted() || isPast(trip.getDate_time())){ // ispast is static method in Adapter class
            //addNoteBtn.setVisibility(Button.INVISIBLE);
            editTrip.setVisibility(Button.INVISIBLE);
            editTrip.setEnabled(false);
        }

        trip_name.setText(trip.getTrip_name());
        trip_from.setText("From: "+trip.getStart_name());
        trip_to.setText("To: "+trip.getEnd_name());
        trip_date_time.setText("Date: "+trip.getDate_time());
        if(trip.isRepeated())
            repeated.setImageResource(R.drawable.yes);
        else
            repeated.setImageResource(R.drawable.no);
        if(trip.isRoundtrip())
            roundTrip.setImageResource(R.drawable.yes);
        else
            roundTrip.setImageResource(R.drawable.no);

        // Dynamicly add notes
        //LayoutInflater inflater = getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for(int i=0;i<trip.getNotes().size();i++) {
            View dynamic_view = inflater.inflate(R.layout.dynamic_note_row,null);
            TextView textView = dynamic_view.findViewById(R.id.note);
            textView.setText(trip.getNotes().get(i));
            //if(dynamic_view.getParent()!=null)
            //    ((ViewGroup)dynamic_view.getParent()).removeView(dynamic_view);
            notes.addView(dynamic_view);
            Log.i(TAG , "note "+i);
        }

        editTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToEditActivity();
            }
        });

        startTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  Toast.makeText( context, tripData.getTrip_name(), Toast.LENGTH_LONG).show();
                //int itemPosition = recyclerView.getChildLayoutPosition(v);
                //Trip tripData = tripDataList.get(itemPosition);
                // Trip tripData = tripDataList.get(0);
                Trip tempTirp = trip;
                trip.setStarted(true);
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?saddr=%f,%f(%s)&daddr=%f,%f (%s)", trip.getStart_lat(), trip.getStart_long(), trip.getStart_name(),  trip.getEnd_lat() , trip.getEnd_long(), trip.getEnd_name());
                if(trip.isRoundtrip()){
                    trip.setStarted(false);
                    trip.setRoundtrip(false);
                    // add 2 hours
                    final long millisToAdd = 7_200_000; //two hours
                    Date d = trip.getDate_time();
                    d.setTime(d.getTime() + millisToAdd);
                    trip.setDate_time(d);
                    // swip lat long
                    double temp_lat = trip.getStart_lat();
                    double temp_long = trip.getStart_long();
                    String temp_name = trip.getStart_name();

                    trip.setStart_name(trip.getEnd_name());
                    trip.setStart_lat(trip.getEnd_lat());
                    trip.setStart_long(trip.getEnd_long());

                    trip.setEnd_name(temp_name);
                    trip.setEnd_lat(temp_lat);
                    trip.setEnd_long(temp_long);

                }
                Adapter dbAdapter = new Adapter(getContext());

                dbAdapter.updateTrip(trip);

                //Toast.makeText( context, "Start trip activity", Toast.LENGTH_LONG).show();

                /*Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                getContext().startActivity(intent);*/

                // new code for notes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getContext())) {
                    Intent intent_permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getContext().getPackageName()));
                    intent_permission.putExtra("trip",tempTirp);
                    startActivityForResult(intent_permission, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                } else {
                    initializeView(tempTirp);
                }

            }
        });
        Log.i(TAG , "trip title : "+trip.getTrip_name());
        return view;
    }

    private void goToEditActivity(){
        getActivity().finish();
        Intent intent = new Intent(getActivity(), EditTripActivity.class);
        intent.putExtra("trip",trip);
        startActivity(intent);
    }

    private void initializeView(Trip trip) {
        //List<String> notes = new ArrayList<>();
        //notes = trip.getNotes();
        Intent intent = new Intent(getContext(), NoteHeadService.class);
        //Bundle b = new Bundle();
        //b.putSerializable("trip", trip);
        intent.putExtra("trip", trip.getTrip_id());
        getActivity().finish();
        getActivity().startService(intent);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

            //Check if the permission is granted or not.
            // Settings activity never returns proper value so instead check with following method
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(getContext())) {
                    initializeView((Trip) data.getSerializableExtra("trip"));
                } else { //Permission is not available
                    Toast.makeText(getContext(),
                            "Draw over other app permission not available. Closing the application",
                            Toast.LENGTH_SHORT).show();

                    getActivity().finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    

}
