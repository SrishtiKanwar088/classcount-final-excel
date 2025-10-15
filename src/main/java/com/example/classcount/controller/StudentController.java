package com.example.classcount.controller;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.service.ExcelImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final ExcelImportService excelImportService;

    public StudentController(StudentRepository studentRepository, ClassroomRepository classroomRepository, ExcelImportService excelImportService) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.excelImportService = excelImportService;
    }

    // Display the list of students for a specific year
    @GetMapping("/{year}")
    public String listStudents(@PathVariable("year") String year, Model model) {
        List<Student> students = studentRepository.findByClassroom_Year(year);
        model.addAttribute("students", students);
        model.addAttribute("currentYear", year);
        model.addAttribute("newStudent", new Student()); // For the Add Student form
        return "student-list";
    }

    // Handle the form submission for importing students from Excel
    @PostMapping("/import")
    public String importStudents(@RequestParam("file") MultipartFile file, @RequestParam("year") String year, RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
                return "redirect:/students/" + year;
            }
            excelImportService.importStudentsFromExcel(file, year);
            redirectAttributes.addFlashAttribute("successMessage", "Students imported successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error importing students: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing file: " + e.getMessage());
        }
        return "redirect:/students/" + year;
    }

    // Handle the form for editing a student (displays the pre-filled form)
    @GetMapping("/edit/{id}")
    public String editStudent(@PathVariable("id") Long id, Model model) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student ID:" + id));
        model.addAttribute("student", student);
        model.addAttribute("currentYear", student.getClassroom().getYear());
        return "student-edit";
    }

    // Handle the submission of the edited student
    @PostMapping("/update/{id}")
    public String updateStudent(@PathVariable("id") Long id, @ModelAttribute("student") Student student, RedirectAttributes redirectAttributes) {
        student.setId(id);

        Classroom existingClassroom = classroomRepository.findById(student.getClassroom().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid classroom ID."));
        student.setClassroom(existingClassroom);

        studentRepository.save(student);
        redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");

        // Correct redirect back to the student list for the correct year
        return "redirect:/students/" + existingClassroom.getYear();
    }

    // Handle student deletion
    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student ID:" + id));
        String year = student.getClassroom().getYear();
        studentRepository.delete(student);
        redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");
        return "redirect:/students/" + year;
    }
}
