package tourGuide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Rewards service.
 */
@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final ExecutorService executor;
    private final Logger log = LoggerFactory.getLogger(RewardsService.class);

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;

    /**
     * Instantiates a new Rewards service.
     *
     * @param gpsUtil       the gps util
     * @param rewardCentral the reward central
     */
    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        this.executor = Executors.newFixedThreadPool(100);
    }

    /**
     * Sets proximity buffer.
     *
     * @param proximityBuffer the proximity buffer
     */
    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    /**
     * Sets default proximity buffer.
     */
    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    /**
     * Calculate rewards for visited locations.
     *
     * @param user the user
     * @return the completable future
     */
    public CompletableFuture<Void> calculateRewards(User user) {
        return CompletableFuture.runAsync(() -> {
            log.info("Calculating rewards for user: {}", user.getUserName());

            List<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
            List<Attraction> attractions = gpsUtil.getAttractions();
            for (VisitedLocation visitedLocation : userLocations) {
                for (Attraction attraction : attractions) {
                    if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
                        if (nearAttraction(visitedLocation, attraction)) {
                            int rewardPoints = getRewardPoints(attraction, user);
                            user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
                            log.debug("Added reward for user: {} - Attraction: {} - Points: {}", user.getUserName(), attraction.attractionName, rewardPoints);
                        }
                    }
                }
            }
        }, executor);
    }

    /**
     * Check if is within attraction proximity boolean.
     *
     * @param attraction the attraction
     * @param location   the location
     * @return the boolean
     */
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        log.debug("Checking attraction proximity - Attraction: {} - Location: {}", attraction.attractionName, location);
        return getDistance(attraction, location) <= attractionProximityRange;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        log.debug("Checking if user is near attraction - User: {} - Attraction: {}", visitedLocation.userId, attraction.attractionName);
        return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
    }

    /**
     * Gets reward points.
     *
     * @param attraction the attraction
     * @param user       the user
     * @return the reward points
     */
    int getRewardPoints(Attraction attraction, User user) {
        log.debug("Getting reward points for user: {} - Attraction: {}", user.getUserName(), attraction.attractionName);
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    /**
     * Gets distance.
     *
     * @param loc1 the loc 1
     * @param loc2 the loc 2
     * @return the distance
     */
    public double getDistance(Location loc1, Location loc2) {
        log.debug("Calculating distance - Location 1: {} - Location 2: {}", loc1, loc2);
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);
        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;

        log.debug("Distance calculated - Location 1: {} - Location 2: {} - Distance: {} statute miles", loc1, loc2, statuteMiles);
        return statuteMiles;
    }
}
