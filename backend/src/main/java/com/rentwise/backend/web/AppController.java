package com.rentwise.backend.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AppController {
    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping("/catalog/states")
    public List<String> states() {
        return appService.states();
    }

    @GetMapping("/catalog/cities")
    public List<String> cities(@RequestParam String state) {
        return appService.cities(state);
    }

    @GetMapping("/catalog/localities")
    public List<String> localities(@RequestParam String state, @RequestParam String city) {
        return appService.localities(state, city);
    }

    @GetMapping("/properties")
    public List<PropertyCardDto> search(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String locality
    ) {
        return appService.search(state, city, locality);
    }

    @GetMapping("/properties/{propertyId}")
    public PropertyDetailDto propertyDetail(@PathVariable Long propertyId, Authentication authentication) {
        return appService.propertyDetail(propertyId, authentication);
    }

    @PostMapping("/properties")
    public PropertyDetailDto createProperty(
            @Valid @RequestBody CreatePropertyCommand command,
            Authentication authentication
    ) {
        return appService.createProperty(command, authentication);
    }

    @PostMapping("/properties/{propertyId}/reviews")
    public ReviewDto createReview(
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateReviewCommand command,
            Authentication authentication
    ) {
        return appService.createReview(propertyId, command, authentication);
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public ReviewCommentDto addReply(
            @PathVariable Long reviewId,
            @Valid @RequestBody AddReplyCommand command,
            Authentication authentication
    ) {
        return appService.addReply(reviewId, command, authentication);
    }

    @PostMapping("/reviews/{reviewId}/votes")
    public VoteSummaryDto vote(
            @PathVariable Long reviewId,
            @Valid @RequestBody VoteCommand command,
            Authentication authentication
    ) {
        return appService.vote(reviewId, command, authentication);
    }
}
