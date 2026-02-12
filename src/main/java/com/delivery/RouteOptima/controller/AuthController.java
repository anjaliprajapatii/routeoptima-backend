package com.delivery.RouteOptima.controller;

import com.delivery.RouteOptima.entity.User;
import com.delivery.RouteOptima.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Allow Frontend
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // 1. REGISTER (ADMIN)
    // ==========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // Check if email exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered!");
        }
        
        // ✅ Check Mobile (Mandatory)
        if (user.getMobile() == null || user.getMobile().isEmpty()) {
            return ResponseEntity.badRequest().body("Mobile number is required!");
        }

        user.setRole("ADMIN");
        user.setAuthProvider("LOCAL");
        user.setAvailable(true);
        
        userRepository.save(user);
        return ResponseEntity.ok("Admin Registered Successfully!");
    }

    // ==========================================
    // 2. LOGIN
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> user = userRepository.findByEmail(loginRequest.getEmail());
        if (user.isPresent() && user.get().getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.status(401).body("Invalid Email or Password");
    }
    
 // ==========================================
    // ✅ NEW: FORGOT PASSWORD (Reset Logic)
    // ==========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String mobile = request.get("mobile");
        String newPassword = request.get("newPassword");

        // 1. Find User by Email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email not registered!");
        }

        User user = userOpt.get();

        // 2. SECURITY CHECK: Does the Mobile Number match?
        if (user.getMobile() == null || !user.getMobile().equals(mobile)) {
            return ResponseEntity.badRequest().body("❌ Verification Failed: Mobile number incorrect!");
        }

        // 3. Update Password
        user.setPassword(newPassword);
        userRepository.save(user);

        return ResponseEntity.ok("✅ Password Reset Successfully! Please Login.");
    }

    // ==========================================
    // 3. ADD DRIVER (FIXED)
    // ==========================================
    @PostMapping("/add-driver")
    public ResponseEntity<?> addDriver(@RequestBody Map<String, String> request) {
        String adminEmail = request.get("adminEmail");
        String driverName = request.get("driverName");
        String driverEmail = request.get("driverEmail");
        String driverPassword = request.get("driverPassword");
        // ✅ Receive Mobile
        String driverMobile = request.get("mobile"); 

        // 1. Validation for Mobile
        if (driverMobile == null || driverMobile.isEmpty()) {
            return ResponseEntity.badRequest().body("Mobile Number is Mandatory!");
        }

        // 2. Check Admin
        Optional<User> adminOpt = userRepository.findByEmail(adminEmail);
        if (adminOpt.isEmpty() || !adminOpt.get().getRole().equalsIgnoreCase("ADMIN")) {
            return ResponseEntity.badRequest().body("Only Admins can add drivers!");
        }

        // 3. Check Duplicate Driver Email
        if (userRepository.findByEmail(driverEmail).isPresent()) {
            return ResponseEntity.badRequest().body("Driver email already exists!");
        }

        // 4. Create & Save Driver
        User newDriver = new User();
        newDriver.setName(driverName);
        newDriver.setEmail(driverEmail);
        newDriver.setPassword(driverPassword);
        newDriver.setMobile(driverMobile); // ✅ Set Mobile
        newDriver.setRole("DRIVER");
        newDriver.setAuthProvider("LOCAL");
        newDriver.setAvailable(true);
        newDriver.setMyAdminId(adminOpt.get().getId()); // Link to Admin

        userRepository.save(newDriver);
        return ResponseEntity.ok("Driver Added Successfully!");
    }

    // ==========================================
    // 4. GET MY DRIVERS
    // ==========================================
    @GetMapping("/my-drivers")
    public ResponseEntity<?> getMyDrivers(@RequestParam String adminEmail) {
        Optional<User> admin = userRepository.findByEmail(adminEmail);
        if (admin.isPresent()) {
            return ResponseEntity.ok(userRepository.findByRoleAndMyAdminId("DRIVER", admin.get().getId()));
        }
        return ResponseEntity.badRequest().body("Admin not found");
    }

    // ==========================================
    // 5. UPDATE DRIVER
    // ==========================================
    @PutMapping("/update-driver/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<User> driverOpt = userRepository.findById(id);
        if (driverOpt.isEmpty()) return ResponseEntity.badRequest().body("Driver not found!");

        User driver = driverOpt.get();
        
        if (updates.containsKey("name")) driver.setName(updates.get("name"));
        if (updates.containsKey("password")) driver.setPassword(updates.get("password"));
        if (updates.containsKey("mobile")) driver.setMobile(updates.get("mobile")); // ✅ Update Mobile

        // Handle Email Update (Check duplicate)
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (!driver.getEmail().equals(newEmail) && userRepository.findByEmail(newEmail).isPresent()) {
                return ResponseEntity.badRequest().body("Email already in use!");
            }
            driver.setEmail(newEmail);
        }
        
        userRepository.save(driver);
        return ResponseEntity.ok("Driver Updated!");
    }

    // ==========================================
    // 6. DELETE DRIVER
    // ==========================================
    @DeleteMapping("/delete-driver/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ResponseEntity.badRequest().body("Driver not found!");
        userRepository.deleteById(id);
        return ResponseEntity.ok("Driver Deleted");
    }
}