package com.delivery.RouteOptima.repository;

import com.delivery.RouteOptima.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // 1. Find by Status (e.g., Get all PENDING orders)
    List<Order> findByStatus(String status);

    // 2. Get All Orders, sorted by Newest First
    List<Order> findAllByOrderByOrderDateDesc();
}