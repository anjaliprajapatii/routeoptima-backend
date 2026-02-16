package com.delivery.RouteOptima.repository;

import com.delivery.RouteOptima.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // ✅ Naya method: Sirf us Admin ke orders dhoondne ke liye jiska email match kare
    List<Order> findByAdminEmailOrderByOrderDateDesc(String adminEmail);

    List<Order> findAllByOrderByOrderDateDesc();
}