package com.example.al.cameramarkersoverlay;

import android.location.Location;

import com.example.al.cameramarkersoverlay.data.LocationMarker;

import java.util.ArrayList;

// интерфейс для взаимодействия ViewOverlay с фрагментом
public interface InterfaceSensors {

    public double getAzimuth();

    public Location getLocation();

    public ArrayList<LocationMarker> getMarkers();

    public double getPitch();

    public double getRoll();

    public int getScreenRotation();

}
