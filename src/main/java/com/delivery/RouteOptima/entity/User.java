package com.delivery.RouteOptima.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    // ✅ FIXED: Mobile field (Mandatory)
    @Column(unique = true, nullable = false) 
    private String mobile; 

    private String role; // ADMIN or DRIVER
    private String authProvider; // LOCAL, GOOGLE
    
    private boolean isAvailable = true; // For Drivers
    
    private Long myAdminId; // Links Driver to Admin
    
    // ✅ ADDED BACK: Your required fields
    private String currentOrderId;
    private String currentOrderDetails;
    
    // For Tracking Location
    private Double latitude;
    private Double longitude;
}