package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tourGuide.utils.NearbyAttraction;
import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * The type Tour guide service.
 */
@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	/**
	 * The Tracker.
	 */
	public final Tracker tracker;
	/**
	 * The Test mode.
	 */
	boolean testMode = true;
	private final ExecutorService executor;


	/**
	 * Instantiates a new Tour guide service.
	 *
	 * @param gpsUtil        the gps util
	 * @param rewardsService the rewards service
	 */
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		this.executor = Executors.newFixedThreadPool(100);

		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	/**
	 * Gets user rewards.
	 *
	 * @param user the user
	 * @return the user rewards
	 */
	public List<UserReward> getUserRewards(User user) {
		logger.debug("Getting rewards for user: {}", user.getUserName());
		return user.getUserRewards();
	}

	/**
	 * Gets all users last visited locations.
	 *
	 * @return the all users last visited locations
	 */
	public Map<UUID, VisitedLocation> getAllUsersLastVisitedLocations() {
		logger.debug("Getting last visited locations for all users");
		List<User> allUsers = getAllUsers();
		Map<UUID, VisitedLocation> lastVisitedLocations = new HashMap<>();
		for (User user : allUsers) {
			VisitedLocation lastVisitedLocation = getUserLocation(user);
			lastVisitedLocations.put(user.getUserId(), lastVisitedLocation);
		}
		return lastVisitedLocations;
	}

	/**
	 * Gets near by attractions.
	 *
	 * @param visitedLocation the visited location
	 * @return the near by attractions
	 */
	public List<NearbyAttraction> getNearByAttractions(VisitedLocation visitedLocation) {
		logger.debug("Getting nearby attractions for visited location of user: {}", visitedLocation.userId);
		List<Attraction> allAttractions = gpsUtil.getAttractions();
		List<NearbyAttraction> closestAttractions = new ArrayList<>();
		User user = getUserById(visitedLocation.userId);

		for (Attraction attraction : allAttractions) {
			double distance = rewardsService.getDistance(attraction, visitedLocation.location);
			int rewardPoints = rewardsService.getRewardPoints(attraction, user);
			NearbyAttraction nearbyAttraction = new NearbyAttraction(attraction.attractionName, attraction, visitedLocation.location, distance, rewardPoints);
			closestAttractions.add(nearbyAttraction);
		}

		// Sort attractions by distance and pick the closest five
		closestAttractions.sort(Comparator.comparingDouble(NearbyAttraction::getDistanceInKm));
		return closestAttractions.subList(0, Math.min(closestAttractions.size(), 5));
	}

	/**
	 * Gets user location.
	 *
	 * @param user the user
	 * @return the user location
	 */
	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation;
		if (user.getVisitedLocations().size() > 0) {
			visitedLocation = user.getLastVisitedLocation();
		} else {
			try {
				visitedLocation = trackUserLocation(user).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Error getting user location", e);
				throw new RuntimeException("Error getting user location", e);
			}
		}
		logger.debug("Getting location for user: {}", user.getUserName());
		return visitedLocation;
	}

	/**
	 * Gets user.
	 *
	 * @param userName the user name
	 * @return the user
	 */
	public User getUser(String userName) {
		logger.debug("Getting user by username: {}", userName);
		return internalUserMap.get(userName);
	}

	/**
	 * Gets user by id.
	 *
	 * @param userId the user id
	 * @return the user by id
	 */
	public User getUserById(UUID userId) {
		logger.debug("Getting user by ID: {}", userId);
		return internalUserMap.values().stream()
				.filter(user -> user.getUserId().equals(userId))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Gets all users.
	 *
	 * @return the all users
	 */
	public List<User> getAllUsers() {
		logger.debug("Getting all users");
		return new ArrayList<>(internalUserMap.values());
	}

	/**
	 * Add user.
	 *
	 * @param user the user
	 */
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
			logger.debug("Added user: {}", user.getUserName());
		}
	}

	/**
	 * Gets trip deals.
	 *
	 * @param user the user
	 * @return the trip deals
	 */
	public List<Provider> getTripDeals(User user) {
		logger.debug("Getting trip deals for user: {}", user.getUserName());
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Track user location.
	 *
	 * @param user the user
	 * @return the completable future
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
		logger.debug("Tracking location for user: {}", user.getUserName());
		return CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocation);
			return visitedLocation;
		}, executor);
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
}
