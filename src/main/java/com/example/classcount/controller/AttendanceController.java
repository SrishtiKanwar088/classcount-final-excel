package com.example.classcount.controller;

import com.example.classcount.entity.Attendance;
import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import com.example.classcount.repository.AttendanceRepository;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.repository.SubjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassroomRepository classroomRepository;

    public AttendanceController(StudentRepository studentRepository, SubjectRepository subjectRepository, AttendanceRepository attendanceRepository, ClassroomRepository classroomRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.classroomRepository = classroomRepository;
    }

    // Displays the page to select a subject and take attendance
    @GetMapping("/take/{year}")
    public String takeAttendance(@PathVariable("year") String year, Model model) {
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);
        model.addAttribute("subjects", subjects);
        model.addAttribute("currentYear", year);
        return "attendance-take";
    }

    // Handles the selection of a subject and displays the list of students for marking attendance
    @PostMapping("/mark")
    public String markAttendance(@RequestParam("year") String year,
                                 @RequestParam("subjectId") Long subjectId,
                                 Model model) {

        Optional<Subject> subjectOptional = subjectRepository.findById(subjectId);
        if (subjectOptional.isEmpty()) {
            return "redirect:/attendance/take/" + year; // Redirect with error
        }

        List<Student> students = studentRepository.findByClassroom_Year(year);

        // Check for existing attendance for today's date
        LocalDate today = LocalDate.now();
        List<Attendance> existingAttendance = attendanceRepository.findBySubjectAndDate(subjectOptional.get(), today);

        // Map existing attendance to a model attribute for pre-selection
        boolean hasExistingAttendance = !existingAttendance.isEmpty();

        model.addAttribute("students", students);
        model.addAttribute("subject", subjectOptional.get());
        model.addAttribute("hasExistingAttendance", hasExistingAttendance);
        model.addAttribute("currentYear", year);
        model.addAttribute("todayDate", today);

        return "attendance-mark";
    }

    // Handles the form submission to save attendance for all students
    @PostMapping("/save")
    public String saveAttendance(@RequestParam("subjectId") Long subjectId,
                                 @RequestParam("year") String year,
                                 @RequestParam("studentIds") List<Long> studentIds,
                                 @RequestParam(name = "presentStudentIds", required = false) List<Long> presentStudentIds,
                                 RedirectAttributes redirectAttributes) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject ID."));

        LocalDate today = LocalDate.now();

        // Delete any existing attendance for this subject and date before saving new ones
        List<Attendance> existingAttendance = attendanceRepository.findBySubjectAndDate(subject, today);
        attendanceRepository.deleteAll(existingAttendance);

        // Create and save new attendance records
        if (presentStudentIds == null) {
            presentStudentIds = List.of(); // Handle case where no one is present
        }

        for (Long studentId : studentIds) {
            Student student = studentRepository.findById(studentId).get(); // Assuming student exists
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setSubject(subject);
            attendance.setDate(today);
            attendance.setPresent(presentStudentIds.contains(studentId));
            attendanceRepository.save(attendance);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Attendance saved successfully for " + subject.getName() + "!");
        return "redirect:/attendance/take/" + year;
    }
}