package com.epics.smartcityapp;

import android.util.Base64;

/**
 * Created by kartikmittal on 2/26/18.
 */

public class Pothole {
    String latitude;
    String longitude;
    String encodedImage;
    String severity;
    String timeStamp;

    public Pothole(String latitude, String longitude, String encodedImage, String severity, String timeStamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.encodedImage = encodedImage;
        this.severity = severity;
        this.timeStamp = timeStamp;
    }
}
