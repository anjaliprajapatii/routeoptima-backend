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

    @Column(unique = true, nullable = false) 
    private String mobile; 

    private String role; // ADMIN or DRIVER
    private String authProvider; // LOCAL, GOOGLE
    
    private boolean isAvailable = true; // For Drivers
    
    private Long myAdminId; // Links Driver to Admin ID
    
    // ✅ ADDED: This field is mandatory for isolation logic
    private String adminEmail; // Links Driver to Admin Email
    
    private String currentOrderId;
    private String currentOrderDetails;
    
    // For Tracking Location
    private Double latitude;
    private Double longitude;
}