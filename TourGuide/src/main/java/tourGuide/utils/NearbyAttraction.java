package tourGuide.utils;

import gpsUtil.location.Location;

public class NearbyAttraction {
    private String name;
    private Location attractionLocation;
    private Location userLocation;
    private double distanceInMiles;
    private int rewardPoints;

    public NearbyAttraction(String name, Location attractionLocation, Location userLocation, double distanceInMiles, int rewardPoints) {
        this.name = name;
        this.attractionLocation = attractionLocation;
        this.userLocation = userLocation;
        this.distanceInMiles = distanceInMiles;
        this.rewardPoints = rewardPoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getAttractionLocation() {
        return attractionLocation;
    }

    public void setAttractionLocation(Location attractionLocation) {
        this.attractionLocation = attractionLocation;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public double getDistanceInMiles() {
        return distanceInMiles;
    }

    public void setDistanceInMiles(double distanceInMiles) {
        this.distanceInMiles = distanceInMiles;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }
}
