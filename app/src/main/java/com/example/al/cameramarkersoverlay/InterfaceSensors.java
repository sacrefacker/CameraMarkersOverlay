package com.example.al.cameramarkersoverlay;

import android.location.Location;

import java.util.ArrayList;

// интерфейс для взаимодействия ViewOverlay с фрагментом
public interface InterfaceSensors {

    public double getAzimuth();

    public Location getLocation();

    public ArrayList<Location> getMarkers();

    public double getPitch();

    public double getRoll();

    public int getScreenRotation();

}
