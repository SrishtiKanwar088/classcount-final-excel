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

    @GetMapping("/{year}")
    public String viewAttendance(@PathVariable("year") String year, Model model) {
        Map<String, Object> summary = attendanceService.getAttendanceSummaryForYear(year);
        model.addAllAttributes(summary);
        model.addAttribute("currentYear", year);
        return "attendance-view";
    }


    @PostMapping("/reset/{year}")
    public String resetAttendance(@PathVariable("year") String year, RedirectAttributes redirectAttributes) {
        try {
            // 1. Fetch only the students belonging to the specific year being viewed
            List<Student> studentsInYear = studentRepository.findByClassroom_Year(year);

            // 2. Extract their IDs
            List<Long> studentIds = studentsInYear.stream().map(Student::getId).collect(Collectors.toList());

            if (studentIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No students found in " + year + " to reset records.");
                return "redirect:/attendance/view/" + year;
            }

            // 3. Delete all attendance records associated with those specific student IDs
            // The repository method handles the SQL DELETE WHERE student_id IN (...)
            int deletedCount = attendanceRepository.deleteByStudentIdIn(studentIds);

            redirectAttributes.addFlashAttribute("successMessage", deletedCount + " attendance records deleted. Attendance is now reset for " + year + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error resetting attendance: " + e.getMessage());
        }

        // Redirect back to the reports page to show the refreshed data (which should be 0)
        return "redirect:/attendance/view/" + year;
    }


    @GetMapping("/export/{year}")
    public void exportAttendance(@PathVariable("year") String year, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Attendance_" + year.replace(" ", "_") + ".xlsx";
        response.setHeader(headerKey, headerValue);

        List<Student> students = studentRepository.findByClassroom_Year(year);
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);
        List<Attendance> allAttendance = attendanceRepository.findAll().stream()
                .filter(a -> a.getStudent().getClassroom().getYear().equals(year))
                .collect(Collectors.toList());

        excelExportService.exportAttendance(response, students, subjects, allAttendance);
    }
}
