package com.example.classcount.controller;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.repository.AttendanceRepository;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.service.ExcelImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/students")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final ExcelImportService excelImportService;
    private final AttendanceRepository attendanceRepository;

    public StudentController(StudentRepository studentRepository, ClassroomRepository classroomRepository, ExcelImportService excelImportService, AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.excelImportService = excelImportService;
        this.attendanceRepository = attendanceRepository;
    }

    // Displays the list of students for a specific year
    @GetMapping("/{year}")
    public String listStudents(@PathVariable("year") String year, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<Classroom> classroomOptional = classroomRepository.findByYear(year);
            if (classroomOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid classroom year provided.");
                return "redirect:/welcome";
            }

            // CRITICAL FIX: Fetch students by year only (REVERTED)
            List<Student> students = studentRepository.findByClassroom_Year(year);
            model.addAttribute("students", students);
            model.addAttribute("currentYear", year);
            model.addAttribute("currentSection", "N/A"); // Placeholder for clean HTML parsing
            return "student-list";

        } catch (Exception e) {
            logger.error("CRITICAL DB ERROR while loading student list for {}: {}", year, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatal Database Error: Could not load student list. Please try again.");
            return "redirect:/welcome";
        }
    }

    // Handles the Excel file upload and import
    @PostMapping("/import/{year}") // REMOVED {section} from path
    public String importStudents(@PathVariable("year") String year,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid file selection or format.");
                return "redirect:/students/" + year;
            }

            Classroom classroom = classroomRepository.findByYear(year)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid year provided."));

            // CRITICAL FIX: Call service with only two arguments (file, classroom)
            int savedCount = excelImportService.importStudents(file, classroom);

            redirectAttributes.addFlashAttribute("successMessage", savedCount + " students imported successfully into " + year + "!");
        } catch (Exception e) {
            String errorMsg = e.getMessage().contains("Excel data") ? e.getMessage() : "An unexpected error occurred during import. Check file format.";
            redirectAttributes.addFlashAttribute("errorMessage", "Import Failed: " + errorMsg);
        }
        return "redirect:/students/" + year;
    }

    // Deletes all students for the given year
    @PostMapping("/delete-all/{year}") // REMOVED {section} from path
    public String deleteAllStudentsByYearAndSection(@PathVariable("year") String year, RedirectAttributes redirectAttributes) {
        List<Student> students = studentRepository.findByClassroom_Year(year);

        if (students.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No students found in " + year + " to delete.");
            return "redirect:/students/" + year;
        }

        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        attendanceRepository.deleteByStudentIdIn(studentIds);
        studentRepository.deleteAll(students);

        redirectAttributes.addFlashAttribute("successMessage", students.size() + " students and all associated attendance records have been deleted.");

        return "redirect:/students/" + year;
    }


    // Edit methods redirect to the simplified URL structure
    @GetMapping("/edit/{id}")
    public String editStudent(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Student> studentOptional = studentRepository.findById(id);
        if (studentOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Student not found.");
            return "redirect:/welcome";
        }
        Student student = studentOptional.get();
        model.addAttribute("student", student);
        model.addAttribute("currentYear", student.getClassroom().getYear());
        // Setting a placeholder for the Cancel button redirect path
        model.addAttribute("currentSection", "N/A");
        return "student-edit";
    }

    // Update submission redirects to the simplified URL structure
    @PostMapping("/update/{id}")
    public String updateStudent(@PathVariable("id") Long id, @ModelAttribute("student") Student student, RedirectAttributes redirectAttributes) {
        student.setId(id);

        Classroom existingClassroom = classroomRepository.findById(student.getClassroom().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid classroom ID."));
        student.setClassroom(existingClassroom);

        studentRepository.save(student);
        redirectAttributes.addFlashAttribute("successMessage", "Student updated successfully!");

        // Redirect back to the correct year
        return "redirect:/students/" + existingClassroom.getYear();
    }

    // Delete submission redirects to the simplified URL structure
    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<Student> studentOptional = studentRepository.findById(id);
        if (studentOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Student not found.");
            return "redirect:/welcome";
        }
        Student student = studentOptional.get();
        String year = student.getClassroom().getYear();

        List<Long> singleId = List.of(student.getId());
        attendanceRepository.deleteByStudentIdIn(singleId);

        studentRepository.delete(student);
        redirectAttributes.addFlashAttribute("successMessage", "Student deleted successfully!");

        return "redirect:/students/" + year;
    }
}
