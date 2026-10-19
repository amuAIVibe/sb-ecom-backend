package com.ecommerce.project.controller;


import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AuthenticationResult;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest){
        AuthenticationResult authenticationResult = authService.login(loginRequest);

        return ResponseEntity.ok()
                .header(
                    HttpHeaders.SET_COOKIE,
                    authenticationResult.getJwtCookie().toString()
                )
                .body(authenticationResult.getResponse());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        return authService.registerUser(signupRequest);
    }

    @GetMapping("/username")
    public String currentUsername(Authentication authentication) {
        if (authentication != null) {
            return authentication.getName();
        } else {
            return "Please sign in";
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication) {
        return ResponseEntity.ok().body(authService.getCurrentUserDetails(authentication));
    }



    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        ResponseCookie responseCookie = authService.logoutUser();
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        responseCookie.toString()
                )
                .body(new MessageResponse("You've been logged out"));
    }

    @GetMapping("/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
            Integer pageNumber,

            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)
            Integer pageSize,

            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY, required = false)
            String sortBy,

            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false)
            String sortOrder
    ) {
            Sort sortByAndOrder = Sort.by(AppConstants.SORT_USERS_BY).descending();
            Pageable pageDetails = PageRequest.of(pageNumber, Integer.parseInt(AppConstants.PAGE_SIZE), sortByAndOrder);
            return ResponseEntity.ok(authService.getAllSellers(pageDetails));
    }

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // Retrieve or create roles
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseGet(() -> {
                        Role newUserRole = new Role(AppRole.ROLE_USER);
                        return roleRepository.save(newUserRole);
                    });

            Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                    .orElseGet(() -> {
                        Role newSellerRole = new Role(AppRole.ROLE_SELLER);
                        return roleRepository.save(newSellerRole);
                    });

            Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                    .orElseGet(() -> {
                        Role newAdminRole = new Role(AppRole.ROLE_ADMIN);
                        return roleRepository.save(newAdminRole);
                    });

            Set<Role> userRoles = Set.of(userRole);
            Set<Role> sellerRoles = Set.of(sellerRole);
            Set<Role> adminRoles = Set.of(userRole, sellerRole, adminRole);


            // Create users if not already present
            if (!userRepository.existsByUserName("user1")) {
                User user1 = new User("user1", "user1@example.com", passwordEncoder.encode("password1"));
                userRepository.save(user1);
            }

            if (!userRepository.existsByUserName("seller1")) {
                User seller1 = new User("seller1", "seller1@example.com", passwordEncoder.encode("password2"));
                userRepository.save(seller1);
            }

            if (!userRepository.existsByUserName("admin")) {
                User admin = new User("admin", "admin@example.com", passwordEncoder.encode("adminPass"));
                userRepository.save(admin);
            }

            // Update roles for existing users
            userRepository.findByUserName("user1").ifPresent(user -> {
                user.setRoles(userRoles);
                userRepository.save(user);
            });

            userRepository.findByUserName("seller1").ifPresent(seller -> {
                seller.setRoles(sellerRoles);
                userRepository.save(seller);
            });

            userRepository.findByUserName("admin").ifPresent(admin -> {
                admin.setRoles(adminRoles);
                userRepository.save(admin);
            });
        };
    }
}
