package com.example.classcount.controller;

import com.example.classcount.entity.User;
import com.example.classcount.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

// --- CRITICAL FIX: ALL methods in this controller now require the ADMIN role ---
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Displays the user management page with list of all users
    @GetMapping("/manage")
    public String showUserManagement(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("newUser", new User());

        // Roles available for new users (Note: UI shows "ADMIN" but stores "ROLE_ADMIN")
        List<String> roles = List.of("ADMIN", "TEACHER");
        model.addAttribute("roles", roles);

        return "user-management";
    }

    // Handles the form submission to add a new user
    @PostMapping("/add")
    public String addUser(@ModelAttribute("newUser") User newUser, RedirectAttributes redirectAttributes) {
        try {
            if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Username already exists.");
                return "redirect:/users/manage";
            }

            // Hash the password before saving
            String hashedPassword = passwordEncoder.encode(newUser.getPassword());
            newUser.setPassword(hashedPassword);

            // Ensure role is prefixed correctly for Spring Security persistence
            if (!newUser.getRole().startsWith("ROLE_")) {
                newUser.setRole("ROLE_" + newUser.getRole());
            }

            userRepository.save(newUser);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + newUser.getUsername() + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving user: " + e.getMessage());
        }
        return "redirect:/users/manage";
    }

    // Handles user deletion
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Safety check: Cannot delete the last admin user
            if ("ROLE_ADMIN".equals(user.getRole()) && userRepository.count() == 1) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete the last admin user.");
                return "redirect:/users/manage";
            }
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getUsername() + "' deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
        }
        return "redirect:/users/manage";
    }
}
