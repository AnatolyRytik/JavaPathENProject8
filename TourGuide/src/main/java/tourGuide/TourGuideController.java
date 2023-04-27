package tourGuide;

import com.jsoniter.output.JsonStream;
import gpsUtil.location.VisitedLocation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tripPricer.Provider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class TourGuideController {


    private final TourGuideService tourGuideService;

    public TourGuideController(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;
    }

    @GetMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @GetMapping("/getLocation")
    public String getLocation(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        return JsonStream.serialize(visitedLocation.location);
    }

    @GetMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) {
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
        return JsonStream.serialize(tourGuideService.getNearByAttractions(visitedLocation));
    }


    @GetMapping("/getRewards")
    public String getRewards(@RequestParam String userName) {
        return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }

    @GetMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        Map<UUID, VisitedLocation> allUsersLastVisitedLocations = tourGuideService.getAllUsersLastVisitedLocations();
        return JsonStream.serialize(allUsersLastVisitedLocations);
    }

    @GetMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
        List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
        return JsonStream.serialize(providers);
    }

    @PostMapping("/edit/{userId}")
    public ResponseEntity<?> editUserPreferences(@PathVariable UUID userId, @RequestBody UserPreferences newPreferences) {
        User user = tourGuideService.getUserById(userId);
        if (user == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        user.setUserPreferences(newPreferences);

        return new ResponseEntity<>(newPreferences, HttpStatus.OK);
    }

    private User getUser(String userName) {
        return tourGuideService.getUser(userName);
    }


}