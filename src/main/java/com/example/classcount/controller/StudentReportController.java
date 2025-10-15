package com.example.classcount.controller;

import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import com.example.classcount.repository.AttendanceRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.repository.SubjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/reports/student")
public class StudentReportController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;

    public StudentReportController(StudentRepository studentRepository,
                                   AttendanceRepository attendanceRepository,
                                   SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
    }

    // ✅ FIXED: Removed 'section' from the URL mapping
    @GetMapping("/{year}/{rollNumber}")
    public String viewIndividualReport(@PathVariable("year") String year,
                                       @PathVariable("rollNumber") String rollNumber,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        // Try to find the student by roll number and year
        Optional<Student> studentOptional = studentRepository.findByRollNumberAndClassroom_Year(rollNumber, year);

        if (studentOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error: Student with Roll Number " + rollNumber + " not found in " + year + ".");
            return "redirect:/welcome";
        }

        Student student = studentOptional.get();
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);

        Map<String, String> subjectAttendanceSummary = new HashMap<>();
        int overallPresent = 0;
        int overallTotal = 0;

        for (Subject subject : subjects) {
            long presentCount = attendanceRepository.findByStudentAndSubject(student, subject)
                    .stream().filter(a -> a.isPresent()).count();
            long total = attendanceRepository.findByStudentAndSubject(student, subject).size();

            subjectAttendanceSummary.put(subject.getName(), presentCount + "/" + total);
            overallPresent += presentCount;
            overallTotal += total;
        }

        String overallPercentage = overallTotal > 0
                ? String.format("%.2f%%", (double) overallPresent / overallTotal * 100)
                : "0.00%";

        model.addAttribute("student", student);
        model.addAttribute("currentYear", year);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectAttendanceSummary", subjectAttendanceSummary);
        model.addAttribute("overallPercentage", overallPercentage);

        return "individual-report";
    }
}
