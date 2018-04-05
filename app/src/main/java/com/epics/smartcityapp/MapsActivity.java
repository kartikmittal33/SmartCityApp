package com.epics.smartcityapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//main activity to implement maps and camera functionality
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int CAMERA_REQUEST = 1888;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    Spinner spinner;  //severity chooser object
    // EditText edtAddress;  //input address bar
    String autoSelect;
    Button search;  //search address bar
    Button send;
    ImageButton refresh; // button for refresh
    private GoogleMap mMap;  //map object
    private TextView popup;
    double lat = 40.424544;  // latitude
    double lng = -86.918871;  // longitude
    String encodedImage = "";           //converting image to BASE64 to send it to server
                                        // read it from website and decrypting it to get image back
    String severity = "Not selected";
    String timeStamp;


    GPSTracker gpsTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);  //implement maps fragment
        autoSelect = "Purdue Memorial Union, Grant Street, West Lafayette, IN";
        spinner = findViewById(R.id.simplespinner);  //initialize spinner
        // edtAddress = (EditText) findViewById(R.id.btn_place);       //initialize address bar
        search =  findViewById(R.id.search);        //initialize search bar
        send = findViewById(R.id.send);

        refresh = findViewById(R.id.refresh);


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.severity, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setPrompt("Select");
        spinner.setAdapter(
                new NothingSelectedSpinnerAdapter(
                        adapter,
                        R.layout.contact_spinner_row_nothing_selected,
                        // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
                        this));
       // spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getBaseContext(), "Please select something", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
    //            Toast.makeText(getBaseContext(), "Please select something", Toast.LENGTH_LONG).show();
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                autoSelect = place.getAddress().toString();

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });

        // Create class object
        gpsTracker = new GPSTracker(MapsActivity.this);

        // Check if GPS enabled
        if(gpsTracker.canGetLocation()) {

            lat = gpsTracker.getLatitude();
            lng = gpsTracker.getLongitude();

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + lat + "\nLong: " + lng, Toast.LENGTH_LONG).show();

        } else {
            // Can't get location.
            // GPS or network is not enabled.
            // Ask user to enable GPS/network in settings.
            gpsTracker.showSettingsAlert();
        }



        //for refreshing the app

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent starter = getIntent();
                finish();
                startActivity(starter);
            }
        });

    }

    public void onDataSent(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setCancelable(true);
        builder.setTitle(null);
        builder.setMessage("The data has been sent");

        //get the time when the data was sent and store it in a string

        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        timeStamp = simpleDateFormat.format(date);

        //if the user has not selected severity or clicked picture or both
        //send an alert and don't send the data to the server

        if (spinner.getSelectedItem() == null  && encodedImage.equals("")) {
            builder.setMessage("Severity not selected and photo not clicked");
        }
        else if (encodedImage.equals("")) {
            builder.setMessage("Photo not clicked");
        }
        else if (spinner.getSelectedItem() == null) {
            builder.setMessage("Severity not selected");
        }
        else {

            //setting the severity as selected in the spinner
            severity = spinner.getSelectedItem().toString();

            //get the time when the data was sent and store it in a string
            System.out.println(timeStamp);


            //setting the long and lat coordinates to those of center of map/screen
            LatLng coordinates = mMap.getCameraPosition().target;
            lat = coordinates.latitude;
            lng = coordinates.longitude;


            //FIREBASE IMPLEMENTATION

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = database.getReference();

            //make a new pothole object to store all the info of pothole to send to the server
            Pothole pothole = new Pothole(Double.toString(lat), Double.toString(lng), encodedImage, severity,timeStamp);

            //get the key from the database to append the new pothole information in the server
            String key = databaseReference.push().getKey();

            //sending the data as a object to the server with the unique key
            databaseReference.child(key).setValue(pothole);

        }

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }


    //create new intent to take a picture
    public void takePic(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }


    public void getCords(View view) {

        new GetCoordinates().execute(autoSelect.replace(" ", "+"));
    }

    //when camera is activated, take picture and return the photo as a bitmap
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap photo;
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
            try {

                //converting bitmap to base64 string to send it to server

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                byte[] byteArray = stream.toByteArray();

                encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

                //the encoded string has newline char, so to replace all newline char with empty string
                encodedImage = encodedImage.replaceAll("\n","");
            }
            catch (NullPointerException e) {
                System.out.println("Cannot covert");
            }


            if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    autoSelect = place.getName().toString();

                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            }

        }



    }

    //create map and center it around west lala
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        LatLng westL = new LatLng(lat, lng); //make a LatLng object for newLatLng method parameter with west lala cords
        mMap.moveCamera(CameraUpdateFactory.newLatLng(westL)); //move map around west lala
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(westL, 17)); //zoom with 18, shows streets but not too zoomed


    }


    //works with HttpDataHandler (Geocoder) class to return cords from address
    private class GetCoordinates extends AsyncTask<String, Void, String> {
        ProgressDialog dialog = new ProgressDialog(MapsActivity.this);

        //while the geocoder is executing, show a please wait message to not overload requests
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("Please wait...");
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }


        @Override
        protected String doInBackground(String... strings) {
            String response;
            try {
                String address = strings[0];
                HttpDataHandler http = new HttpDataHandler();
                String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?address=%s", address);
                response = http.getHTTPData(url);
                return response;
            } catch (Exception ex) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                JSONObject jsonObject = new JSONObject(s);

                System.out.println(s);

                lat = Double.parseDouble(((JSONArray) jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
                        .getJSONObject("location").get("lat").toString());

                lng = Double.parseDouble(((JSONArray) jsonObject.get("results")).getJSONObject(0).getJSONObject("geometry")
                        .getJSONObject("location").get("lng").toString());

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 18));

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }
}