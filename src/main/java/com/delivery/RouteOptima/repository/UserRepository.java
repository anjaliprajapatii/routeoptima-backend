package com.delivery.RouteOptima.repository;

import com.delivery.RouteOptima.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // ✨ NEW METHOD: Sirf wo drivers lao jo "DRIVER" hain aur "Available" hain
    List<User> findByRoleAndIsAvailable(String role, boolean isAvailable);
    
    List<User> findByRoleAndMyAdminId(String role, Long myAdminId);
}