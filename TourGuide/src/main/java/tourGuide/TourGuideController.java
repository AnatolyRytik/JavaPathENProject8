package tourGuide;

import com.jsoniter.output.JsonStream;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tourGuide.service.TourGuideService;
import tourGuide.service.UserService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tripPricer.Provider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class TourGuideController {

    private Logger log = LoggerFactory.getLogger(TourGuideController.class);
    private final TourGuideService tourGuideService;

    public TourGuideController(TourGuideService tourGuideService, UserService userService) {
        this.tourGuideService = tourGuideService;
    }

    @GetMapping("/")
    public String index() {
        log.info("Index endpoint called");
        return "Greetings from TourGuide!";
    }

    /**
     * This method is used to get the location of a specific user.
     *
     * @param userName This is the name of the user.
     * @return String This returns the location of the user in a JSON format.
     */
    @GetMapping("/getLocation")
    public String getLocation(@RequestParam String userName) {
        log.info("getLocation endpoint called with userName {}", userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        return JsonStream.serialize(visitedLocation.location);
    }

    /**
     * This method is used to get nearby attractions for a specific user.
     *
     * @param userName This is the name of the user.
     * @return String This returns nearby attractions in a JSON format.
     */
    @GetMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) {
        log.info("getNearbyAttractions endpoint called with userName {}", userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }

    /**
     * This method is used to get the rewards of a specific user.
     *
     * @param userName This is the name of the user.
     * @return String This returns the rewards of the user in a JSON format.
     */
    @GetMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        log.info("getRewards endpoint called with userName {}", userName);
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }

    /**
     * This method is used to get all current locations for all users.
     *
     * @return String This returns all current locations in a JSON format.
     */
    @GetMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        log.info("getAllCurrentLocations endpoint called");
        Map<UUID, VisitedLocation> allUsersLastVisitedLocations = tourGuideService.getAllUsersLastVisitedLocations();
        return JsonStream.serialize(allUsersLastVisitedLocations);
    }

    /**
     * This method is used to get trip deals for a specific user.
     *
     * @param userName This is the name of the user.
     * @return String This returns trip deals in a JSON format.
     */
    @GetMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        log.info("getTripDeals endpoint called with userName {}", userName);
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }

    /**
     * This method is used to edit the user preferences of a specific user.
     *
     * @param userId         This is the unique identifier of the user.
     * @param newPreferences This is the new user preferences to be set.
     * @return ResponseEntity This returns the status of the operation along with the updated user preferences in case of success.
     */
    @PostMapping("/edit/{userId}")
    public ResponseEntity<?> editUserPreferences(@PathVariable UUID userId, @RequestBody UserPreferences newPreferences) {
        log.info("editUserPreferences endpoint called with userId {}", userId);
        User user = tourGuideService.getUserById(userId);
        if (user == null) {
            log.error("User not found with userId {}", userId);
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        user.setUserPreferences(newPreferences);
        log.info("User preferences updated for userId {}", userId);
        return new ResponseEntity<>(newPreferences, HttpStatus.OK);
    }

    /**
     * This method is used to get a specific user.
     *
     * @param userName This is the name of the user.
     * @return User This returns the user object.
     */
    private User getUser(String userName) {
        log.info("getUser method called with userName {}", userName);
        return tourGuideService.getUser(userName);
    }
}
