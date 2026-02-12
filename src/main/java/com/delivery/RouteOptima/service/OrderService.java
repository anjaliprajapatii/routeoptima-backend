package com.delivery.RouteOptima.service;

import com.delivery.RouteOptima.entity.Order;
import com.delivery.RouteOptima.entity.User;
import com.delivery.RouteOptima.repository.OrderRepository;
import com.delivery.RouteOptima.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DistanceService distanceService; // ✅ Uses the 'Brain'

    // ==========================================
    // 1. CREATE ORDER
    // ==========================================
    public Order createOrder(Order order) {
        order.setOrderDate(LocalDateTime.now());
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("PENDING");
        }
        // Phone number is automatically saved because it's in the Entity
        return orderRepository.save(order);
    }

    // ==========================================
    // 2. AUTO-ASSIGN DRIVER (Smart Logic) 🧠
    // ==========================================
    public String assignNearestDriver(Long orderId) {
        // A. Find Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getPickupLat() == null || order.getPickupLng() == null) {
            return "Error: Order missing GPS coordinates!";
        }

        // B. Get Available Drivers
        List<User> allDrivers = userRepository.findAll();
        List<User> availableDrivers = allDrivers.stream()
                .filter(u -> "DRIVER".equalsIgnoreCase(u.getRole()) && u.isAvailable())
                .collect(Collectors.toList());

        if (availableDrivers.isEmpty()) {
            return "No drivers are currently available.";
        }

        // C. Find Nearest using DistanceService
        User bestDriver = distanceService.findNearestDriver(order.getPickupLat(), order.getPickupLng(), availableDrivers);

        if (bestDriver == null) {
            return "Available drivers do not have GPS location set.";
        }

        // D. Assign Driver
        order.setAssignedDriver(bestDriver);
        order.setStatus("ASSIGNED");

        bestDriver.setAvailable(false);
        bestDriver.setCurrentOrderId("ORD-" + order.getId());
        bestDriver.setCurrentOrderDetails(order.getItems());

        userRepository.save(bestDriver);
        orderRepository.save(order);

        return "Success: Assigned to " + bestDriver.getName();
    }

    // ==========================================
    // 3. MANUAL ASSIGN
    // ==========================================
    public Order manualAssign(Long orderId, Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        order.setAssignedDriver(driver);
        order.setStatus("ASSIGNED");

        driver.setAvailable(false);
        driver.setCurrentOrderId("ORD-" + order.getId());
        driver.setCurrentOrderDetails(order.getItems());
        
        userRepository.save(driver);
        return orderRepository.save(order);
    }
}