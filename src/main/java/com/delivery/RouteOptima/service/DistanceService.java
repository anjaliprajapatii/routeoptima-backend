package com.delivery.RouteOptima.service;

import com.delivery.RouteOptima.entity.User;
import com.delivery.RouteOptima.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DistanceService {

    public User findNearestDriver(double pickupLat, double pickupLng, List<User> availableDrivers) {
        User nearestDriver = null;
        double minDistance = Double.MAX_VALUE;

        for (User driver : availableDrivers) {
            // ✅ SAFETY CHECK: If driver has NO location, skip them (Don't crash)
            if (driver.getLatitude() == null || driver.getLongitude() == null) {
                continue; 
            }

            double distance = DistanceCalculator.calculateDistance(
                pickupLat, pickupLng, 
                driver.getLatitude(), driver.getLongitude()
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                nearestDriver = driver;
            }
        }
        return nearestDriver;
    }
}