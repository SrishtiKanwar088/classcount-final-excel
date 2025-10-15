package com.example.classcount.controller;

import com.example.classcount.entity.Classroom;
import com.example.classcount.repository.ClassroomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WelcomeController {

    private final ClassroomRepository classroomRepository;

    public WelcomeController(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }


    @GetMapping("/")
    public String showLandingPage() {
        return "landing";
    }


    @GetMapping("/welcome")
    public String showWelcomePage(Model model) {
        List<Classroom> classrooms = classroomRepository.findAll();
        model.addAttribute("classrooms", classrooms);
        return "welcome";
    }
}
