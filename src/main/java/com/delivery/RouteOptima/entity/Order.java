package com.delivery.RouteOptima.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "orders")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String address;
	private String customerName;
	
	// ✅ ADDED: Customer Phone Number
	private String customerPhone; 

	private String items;
	private Double pickupLat;
	private Double pickupLng;
	private Double dropLat;
	private Double dropLng;
	private Double price;
	private LocalDateTime orderDate = LocalDateTime.now();
	private String status; // PENDING, ASSIGNED, DELIVERED

	@ManyToOne
	@JoinColumn(name = "driver_id")
	private User assignedDriver;
}