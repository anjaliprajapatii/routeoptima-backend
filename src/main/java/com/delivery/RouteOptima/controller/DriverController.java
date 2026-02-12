package com.delivery.RouteOptima.controller;

import com.delivery.RouteOptima.entity.Order;
import com.delivery.RouteOptima.entity.User;
import com.delivery.RouteOptima.repository.OrderRepository;
import com.delivery.RouteOptima.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository; // ✅ Need this to fetch Order details

    // ==========================================
    // 1. UPDATE LIVE LOCATION
    // ==========================================
    @PutMapping("/update-location/{driverId}")
    public ResponseEntity<?> updateLocation(@PathVariable Long driverId, @RequestBody Map<String, Double> location) {
        User driver = userRepository.findById(driverId).orElse(null);
        if (driver == null) return ResponseEntity.badRequest().body("Driver ID not found!");

        Double lat = location.get("latitude");
        Double lng = location.get("longitude");

        if (lat != null && lng != null) {
            driver.setLatitude(lat);
            driver.setLongitude(lng);
            userRepository.save(driver);
            return ResponseEntity.ok("Location Updated");
        }
        return ResponseEntity.badRequest().body("Invalid Coordinates");
    }

    // ==========================================
    // 2. GET MY ASSIGNED ORDER (The Fix ✅)
    // ==========================================
    @GetMapping("/{driverId}/current-order")
    public ResponseEntity<?> getDriverOrder(@PathVariable Long driverId) {
        // 1. Find Driver
        User driver = userRepository.findById(driverId).orElse(null);
        if (driver == null) return ResponseEntity.badRequest().body("Driver not found");

        // 2. Check if Driver has an Order ID (e.g., "ORD-5")
        String currentOrderId = driver.getCurrentOrderId();
        
        if (currentOrderId != null && !currentOrderId.isEmpty()) {
            try {
                // 3. Extract ID (Remove "ORD-")
                String cleanId = currentOrderId.replace("ORD-", "");
                Long orderId = Long.parseLong(cleanId);

                // 4. Fetch the Real Order Data
                Order order = orderRepository.findById(orderId).orElse(null);
                return ResponseEntity.ok(order); // Return the Order Object
            } catch (Exception e) {
                return ResponseEntity.ok(null); // Invalid ID format
            }
        }

        return ResponseEntity.ok(null); // No order assigned
    }
}