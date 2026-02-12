package com.delivery.RouteOptima.controller;

import com.delivery.RouteOptima.entity.Order;
import com.delivery.RouteOptima.entity.User;
import com.delivery.RouteOptima.repository.OrderRepository;
import com.delivery.RouteOptima.repository.UserRepository;
import com.delivery.RouteOptima.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // 1. CREATE ORDER
    // ==========================================
    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    // ==========================================
    // 2. GET ALL ORDERS (Sorted by Newest)
    // ==========================================
    @GetMapping("/all")
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    // ==========================================
    // 3. GET ONE ORDER
    // ==========================================
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found")));
    }

    // ==========================================
    // 4. UPDATE ORDER
    // ==========================================
    @PutMapping("/update/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order details) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        if(details.getCustomerName() != null) order.setCustomerName(details.getCustomerName());
        if(details.getCustomerPhone() != null) order.setCustomerPhone(details.getCustomerPhone());
        if(details.getAddress() != null) order.setAddress(details.getAddress());
        if(details.getItems() != null) order.setItems(details.getItems());
        if(details.getStatus() != null) order.setStatus(details.getStatus());

        return ResponseEntity.ok(orderRepository.save(order));
    }

    // ==========================================
    // 5. GET AVAILABLE DRIVERS FOR AN ORDER (Smart Logic)
    // ==========================================
    @GetMapping("/{orderId}/available-drivers")
    public ResponseEntity<?> getAvailableDriversForOrder(@PathVariable Long orderId) {
        // 1. Get the Order to know pickup location
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // 2. Get ALL Drivers who are marked AVAILABLE
        List<User> drivers = userRepository.findAll().stream()
                .filter(u -> "DRIVER".equalsIgnoreCase(u.getRole()) && u.isAvailable())
                .collect(Collectors.toList());

        // 3. Calculate Distance
        List<Map<String, Object>> result = drivers.stream().map(driver -> {
            double distance = 0.0;
            // Only calculate if both have GPS set
            if(order.getPickupLat() != null && order.getPickupLng() != null 
               && driver.getLatitude() != null && driver.getLongitude() != null) {
                distance = calculateDistance(
                    order.getPickupLat(), order.getPickupLng(),
                    driver.getLatitude(), driver.getLongitude()
                );
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", driver.getId());
            map.put("name", driver.getName());
            
            // ✅ FIXED: Using getMobile() instead of getPhone()
            map.put("phone", driver.getMobile()); 
            
            map.put("distanceKm", Math.round(distance * 10.0) / 10.0); // Round to 1 decimal
            return map;
        })
        .sorted(Comparator.comparingDouble(m -> (Double) m.get("distanceKm"))) // Sort Nearest First
        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // Helper: Haversine Formula for Distance Calculation
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // ==========================================
    // 6. MANUAL ASSIGN
    // ==========================================
    @PutMapping("/assign/{orderId}/{driverId}")
    public ResponseEntity<Order> assignDriver(@PathVariable Long orderId, @PathVariable Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        // A. Update Order
        order.setAssignedDriver(driver);
        order.setStatus("ASSIGNED");

        // B. Update Driver (Mark as BUSY)
        driver.setAvailable(false);
        driver.setCurrentOrderId("ORD-" + order.getId()); 
        driver.setCurrentOrderDetails(order.getItems());
        
        // C. SAVE BOTH
        userRepository.save(driver); 
        Order savedOrder = orderRepository.save(order); 
        
        return ResponseEntity.ok(savedOrder);
    }

    // ==========================================
    // 7. AUTO ASSIGN
    // ==========================================
    @PutMapping("/auto-assign/{orderId}")
    public ResponseEntity<?> autoAssignDriver(@PathVariable Long orderId) {
        String result = orderService.assignNearestDriver(orderId);
        
        if (result.startsWith("Error") || result.startsWith("No")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // 8. COMPLETE ORDER (Driver App)
    // ==========================================
    @PutMapping("/complete/{orderId}")
    public ResponseEntity<String> completeOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // A. Mark Order Completed
        order.setStatus("DELIVERED");
        
        // B. Free the Driver (Mark as AVAILABLE)
        if (order.getAssignedDriver() != null) {
            User driver = order.getAssignedDriver();
            driver.setAvailable(true); // Driver is free again!
            driver.setCurrentOrderId(null);
            driver.setCurrentOrderDetails(null);
            userRepository.save(driver); // Update DB
        }

        orderRepository.save(order);
        return ResponseEntity.ok("Order Delivered Successfully");
    }

    // ==========================================
    // 9. DELETE ORDER
    // ==========================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        // Free driver if assigned
        if (order.getAssignedDriver() != null) {
            User driver = order.getAssignedDriver();
            driver.setAvailable(true);
            driver.setCurrentOrderId(null);
            driver.setCurrentOrderDetails(null);
            userRepository.save(driver);
        }

        orderRepository.delete(order);
        return ResponseEntity.ok("Deleted");
    }
 // ==========================================
    // 10. RE-ASSIGN DRIVER (Change Driver)
    // ==========================================
    @PutMapping("/reassign/{orderId}/{newDriverId}")
    public ResponseEntity<Order> reassignDriver(@PathVariable Long orderId, @PathVariable Long newDriverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        User newDriver = userRepository.findById(newDriverId)
                .orElseThrow(() -> new RuntimeException("New Driver not found"));

        // 1. FREE THE OLD DRIVER (If one exists)
        if (order.getAssignedDriver() != null) {
            User oldDriver = order.getAssignedDriver();
            oldDriver.setAvailable(true); // Mark free
            oldDriver.setCurrentOrderId(null);
            oldDriver.setCurrentOrderDetails(null);
            userRepository.save(oldDriver);
        }

        // 2. ASSIGN THE NEW DRIVER
        order.setAssignedDriver(newDriver);
        order.setStatus("ASSIGNED");

        newDriver.setAvailable(false); // Mark busy
        newDriver.setCurrentOrderId("ORD-" + order.getId());
        newDriver.setCurrentOrderDetails(order.getItems());
        
        // 3. SAVE EVERYTHING
        userRepository.save(newDriver);
        Order savedOrder = orderRepository.save(order);
        
        System.out.println("✅ Re-assigned Order " + orderId + " to Driver " + newDriver.getName());
        return ResponseEntity.ok(savedOrder);
    }
}