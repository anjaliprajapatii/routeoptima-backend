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

    // ==========================================
    // 1. CREATE ORDER (Saves with adminEmail)
    // ==========================================
    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // Backend ensure karega ki order create hote waqt saari details sahi hon
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    // ==========================================
    // 2. GET MY ORDERS (Strict Isolation ✅)
    // ==========================================
    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(@RequestParam String adminEmail) {
        // Sirf wahi orders aayenge jiska adminEmail match karega
        List<Order> orders = orderRepository.findByAdminEmailOrderByOrderDateDesc(adminEmail);
        return ResponseEntity.ok(orders);
    }

    // ==========================================
    // 3. UPDATE COORDINATES (Driver Manual Drop ✅)
    // ==========================================
    @PutMapping("/update-coords/{id}")
    public ResponseEntity<Order> updateOrderCoords(@PathVariable Long id, @RequestBody Map<String, Double> coords) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Driver jab map par long-press karega, coordinates yahan update honge
        if(coords.containsKey("dropLat")) order.setDropLat(coords.get("dropLat"));
        if(coords.containsKey("dropLng")) order.setDropLng(coords.get("dropLng"));
        
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // ==========================================
    // 4. ASSIGN DRIVER (Manual)
    // ==========================================
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
    // 5. GET AVAILABLE DRIVERS (Nearest First)
    // ==========================================
    @GetMapping("/{orderId}/available-drivers")
    public ResponseEntity<?> getAvailableDriversForOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        List<User> drivers = userRepository.findAll().stream()
                .filter(u -> "DRIVER".equalsIgnoreCase(u.getRole()) && u.isAvailable())
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

    // ==========================================
    // 6. COMPLETE ORDER (Driver Action)
    // ==========================================
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

    // ==========================================
    // 7. DELETE ORDER
    // ==========================================
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

    // Distance Helper (Haversine)
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