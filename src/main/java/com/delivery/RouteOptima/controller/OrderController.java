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

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(@RequestParam String adminEmail) {
        List<Order> orders = orderRepository.findByAdminEmailOrderByOrderDateDesc(adminEmail);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/update-coords/{id}")
    public ResponseEntity<Order> updateOrderCoords(@PathVariable Long id, @RequestBody Map<String, Double> coords) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if(coords.containsKey("dropLat")) order.setDropLat(coords.get("dropLat"));
        if(coords.containsKey("dropLng")) order.setDropLng(coords.get("dropLng"));
        
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PutMapping("/assign/{orderId}/{driverId}")
    public ResponseEntity<Order> assignDriver(@PathVariable Long orderId, @PathVariable Long driverId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User driver = userRepository.findById(driverId).orElseThrow();

        order.setAssignedDriver(driver);
        order.setStatus("ASSIGNED");

        driver.setAvailable(false);
        driver.setCurrentOrderId("ORD-" + order.getId()); 
        
        userRepository.save(driver); 
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // ==========================================
    // 5. GET AVAILABLE DRIVERS (Nearest + ADMIN ISOLATION ✅ FIXED)
    // ==========================================
    @GetMapping("/{orderId}/available-drivers")
    public ResponseEntity<?> getAvailableDriversForOrder(
            @PathVariable Long orderId, 
            @RequestParam String adminEmail) { 
        
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        List<User> drivers = userRepository.findAll().stream()
                .filter(u -> "DRIVER".equalsIgnoreCase(u.getRole()) 
                        && u.isAvailable() 
                        && adminEmail.equals(u.getAdminEmail())) // ✅ CHANGED TO getAdminEmail()
                .collect(Collectors.toList());

        List<Map<String, Object>> result = drivers.stream().map(driver -> {
            double distance = 0.0;
            if(order.getPickupLat() != null && driver.getLatitude() != null) {
                distance = calculateDistance(order.getPickupLat(), order.getPickupLng(),
                                           driver.getLatitude(), driver.getLongitude());
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id", driver.getId());
            map.put("name", driver.getName());
            map.put("phone", driver.getMobile());
            map.put("distanceKm", Math.round(distance * 10.0) / 10.0);
            return map;
        })
        .sorted(Comparator.comparingDouble(m -> (Double) m.get("distanceKm")))
        .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/reassign/{orderId}/{newDriverId}")
    public ResponseEntity<Order> reassignDriver(@PathVariable Long orderId, @PathVariable Long newDriverId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        User newDriver = userRepository.findById(newDriverId).orElseThrow();

        if (order.getAssignedDriver() != null) {
            User oldDriver = order.getAssignedDriver();
            oldDriver.setAvailable(true);
            oldDriver.setCurrentOrderId(null);
            userRepository.save(oldDriver);
        }

        order.setAssignedDriver(newDriver);
        newDriver.setAvailable(false);
        newDriver.setCurrentOrderId("ORD-" + order.getId());

        userRepository.save(newDriver);
        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PutMapping("/complete/{orderId}")
    public ResponseEntity<String> completeOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus("DELIVERED");
        
        if (order.getAssignedDriver() != null) {
            User driver = order.getAssignedDriver();
            driver.setAvailable(true);
            driver.setCurrentOrderId(null);
            userRepository.save(driver);
        }
        orderRepository.save(order);
        return ResponseEntity.ok("Order Delivered Successfully");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        if (order.getAssignedDriver() != null) {
            User d = order.getAssignedDriver();
            d.setAvailable(true);
            d.setCurrentOrderId(null);
            userRepository.save(d);
        }
        orderRepository.delete(order);
        return ResponseEntity.ok("Deleted");
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}