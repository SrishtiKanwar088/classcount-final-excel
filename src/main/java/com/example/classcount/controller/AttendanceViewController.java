package com.example.classcount.controller;

import com.example.classcount.entity.Attendance;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import com.example.classcount.repository.AttendanceRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.repository.SubjectRepository;
import com.example.classcount.service.AttendanceService;
import com.example.classcount.service.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendance/view")
public class AttendanceViewController {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExcelExportService excelExportService;
    private final AttendanceService attendanceService;

    public AttendanceViewController(StudentRepository studentRepository, SubjectRepository subjectRepository, AttendanceRepository attendanceRepository, ExcelExportService excelExportService, AttendanceService attendanceService) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.excelExportService = excelExportService;
        this.attendanceService = attendanceService;
    }

    // MODIFIED: URL only uses {year}
    @GetMapping("/{year}")
    public String viewAttendance(@PathVariable("year") String year, Model model) {
        // CRITICAL FIX: Service call must only pass 'year'
        Map<String, Object> summary = attendanceService.getAttendanceSummaryForYear(year);

        // Fetch students by year only
        List<Student> studentsInYear = studentRepository.findByClassroom_Year(year);

        model.addAllAttributes(summary);
        model.addAttribute("students", studentsInYear);
        model.addAttribute("currentYear", year);

        return "attendance-view";
    }

    // MODIFIED: URL only uses {year} for deletion scope
    @PostMapping("/reset/{year}")
    public String resetAttendance(@PathVariable("year") String year, RedirectAttributes redirectAttributes) {
        try {
            // Fetch students only for the specific year
            List<Student> studentsInYear = studentRepository.findByClassroom_Year(year);

            List<Long> studentIds = studentsInYear.stream().map(Student::getId).collect(Collectors.toList());

            if (studentIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No students found in " + year + " to reset records.");
                return "redirect:/attendance/view/" + year;
            }

            // Execute batch deletion
            int deletedCount = attendanceRepository.deleteByStudentIdIn(studentIds);

            redirectAttributes.addFlashAttribute("successMessage", deletedCount + " attendance records deleted. Attendance is now reset for " + year + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error resetting attendance: " + e.getMessage());
        }

        // Redirect back to the correct report page
        return "redirect:/attendance/view/" + year;
    }


    // MODIFIED: URL only uses {year} for export
    @GetMapping("/export/{year}")
    public void exportAttendance(@PathVariable("year") String year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        String headerKey = "Content-Disposition";
        // File name simplified
        String headerValue = "attachment; filename=Attendance_" + year.replace(" ", "_") + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // Fetch students only for the specific year
        List<Student> students = studentRepository.findByClassroom_Year(year);
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);

        // Filter attendance records to match only the students in the selected year
        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());

        List<Attendance> allAttendance = attendanceRepository.findAll().stream()
                .filter(a -> studentIds.contains(a.getStudent().getId()))
                .collect(Collectors.toList());

        // We pass the filtered student list and the filtered attendance list to the Excel service
        excelExportService.exportAttendance(response, students, subjects, allAttendance);
    }
}
