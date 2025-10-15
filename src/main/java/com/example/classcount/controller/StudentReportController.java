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

    public StudentReportController(StudentRepository studentRepository, AttendanceRepository attendanceRepository, SubjectRepository subjectRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
    }

    @GetMapping("/{year}/{rollNumber}")
    public String viewIndividualReport(@PathVariable("year") String year,
                                       @PathVariable("rollNumber") String rollNumber,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        // 1. Find the specific student
        Optional<Student> studentOptional = studentRepository.findByRollNumberAndClassroom_Year(rollNumber, year);

        if (studentOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Student with Roll Number " + rollNumber + " not found in " + year + ".");
            return "redirect:/welcome"; // Redirect back to dashboard if student is not found
        }

        Student student = studentOptional.get();
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);

        // 2. Fetch attendance for this student across all subjects
        Map<String, String> subjectAttendanceSummary = new HashMap<>();
        int overallPresent = 0;
        int overallTotal = 0;

        for (Subject subject : subjects) {
            long totalClasses = attendanceRepository.count(); // Placeholder for actual total classes in subject/timeframe
            long presentCount = attendanceRepository.findByStudentAndSubject(student, subject).stream()
                    .filter(a -> a.isPresent())
                    .count();

            // We need the total number of classes this student *should have* attended
            // Since we don't track total classes, we use the total attendance records as the class count for now.
            // A professional fix would be a dedicated 'ClassesTaught' table.
            long totalAttendanceRecords = attendanceRepository.findByStudentAndSubject(student, subject).size();

            String formattedAttendance = presentCount + "/" + totalAttendanceRecords;
            overallPresent += presentCount;
            overallTotal += totalAttendanceRecords;

            subjectAttendanceSummary.put(subject.getName(), formattedAttendance);
        }

        // 3. Calculate Overall Percentage
        String overallPercentage = "0.00%";
        if (overallTotal > 0) {
            double percentage = (double) overallPresent / overallTotal * 100;
            overallPercentage = String.format("%.2f", percentage) + "%";
        }

        // 4. Populate Model for View
        model.addAttribute("student", student);
        model.addAttribute("currentYear", year);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectAttendanceSummary", subjectAttendanceSummary);
        model.addAttribute("overallPercentage", overallPercentage);

        return "individual-report"; // Create this new template next
    }
}
