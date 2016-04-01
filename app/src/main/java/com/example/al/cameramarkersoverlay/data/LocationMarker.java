package com.example.al.cameramarkersoverlay.data;

import android.location.Location;
import android.location.LocationManager;

import com.example.al.cameramarkersoverlay.R;

public class LocationMarker implements Comparable<LocationMarker> {

    private int id = 0;
    private String name = "";
    private Location location = null;
    private String channel = "";
    private String image = "";
    private String date = "";
    private boolean bc = false;
    private float distance = 0.0f;

    public LocationMarker(Location location) {
        this.location = location;
    }

    public LocationMarker(double lat, double lon) {
        this.location = new Location(LocationManager.NETWORK_PROVIDER);
        this.location.setLatitude(lat);
        this.location.setLongitude(lon);
    }

    public String getInfo() {
        return getName() + " " +  getLocation().getLatitude() + " " +  getLocation().getLongitude();
    }

    public Boolean getBc() {
        return bc;
    }

    public void setBc(Boolean bc) {
        this.bc = bc;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(LocationMarker in) {
        if (this.getDistance() == null || in.getDistance() == null) {
            return 0;
        }
        if (this.getDistance().equals(in.getDistance())) {
            return 0;
        }
        return (this.getDistance() - in.getDistance() < 0) ? -1 : 1;
    }
}
