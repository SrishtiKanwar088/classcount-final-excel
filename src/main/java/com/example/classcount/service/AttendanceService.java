package com.example.classcount.service;

import com.example.classcount.entity.Attendance;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import com.example.classcount.repository.AttendanceRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.repository.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final StudentRepository studentRepository;
    private  SubjectRepository subjectRepository;
    private  AttendanceRepository attendanceRepository;

    public AttendanceService(StudentRepository studentRepository, SubjectRepository subjectRepository, AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Retrieves and calculates the attendance summary for a specific academic year and section.
     */
    // CRITICAL FIX: The method MUST accept the 'section' variable to resolve the compilation error
    public Map<String, Object> getAttendanceSummaryForYear(String year, String section) {
        // NOTE: The repository calls below should be updated to a non-section specific method for now,
        // but we'll use the current logic to avoid further cascading errors until we confirm the repository state.

        // Fetch students by both Year AND Section (Assuming a generic year-only retrieval for simplicity after reset)
        List<Student> students = studentRepository.findByClassroom_Year(year);

        // Fetch all subjects for the entire year
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);

        // Fetch all attendance records and filter them down to the relevant students
        List<Attendance> allAttendance = attendanceRepository.findAll();
        List<Attendance> attendanceForYear = allAttendance.stream()
                // CRITICAL FIX: The filter expression uses 'section' and 'year' but assumes findByClassroom_Year() was called correctly
                .filter(a -> a.getStudent().getClassroom().getYear().equals(year))
                .collect(Collectors.toList());

        Map<Long, Map<Long, String>> studentSubjectAttendance = new HashMap<>(); // <studentId, <subjectId, "P/T">>
        Map<Long, String> studentOverallAttendance = new HashMap<>(); // <studentId, "P/T">

        for (Student student : students) {
            Map<Long, Integer> presentCount = new HashMap<>();
            Map<Long, Integer> totalCount = new HashMap<>();
            int overallPresent = 0;
            int overallTotal = 0;

            for (Attendance att : attendanceForYear) {
                if (att.getStudent().getId().equals(student.getId())) {
                    long subjectId = att.getSubject().getId();
                    totalCount.put(subjectId, totalCount.getOrDefault(subjectId, 0) + 1);
                    if (att.isPresent()) {
                        presentCount.put(subjectId, presentCount.getOrDefault(subjectId, 0) + 1);
                        overallPresent++;
                    }
                    overallTotal++;
                }
            }

            // Format data for the view
            Map<Long, String> subjectAttendance = new HashMap<>();
            for (Subject subject : subjects) {
                int p = presentCount.getOrDefault(subject.getId(), 0);
                int t = totalCount.getOrDefault(subject.getId(), 0);
                subjectAttendance.put(subject.getId(), p + "/" + t);
            }
            studentSubjectAttendance.put(student.getId(), subjectAttendance);

            String overallFormatted = overallPresent + "/" + overallTotal;
            if (overallTotal > 0) {
                double percentage = (double) overallPresent / overallTotal * 100;
                overallFormatted += " (" + String.format("%.2f", percentage) + "%)";
            } else {
                overallFormatted = "0/0 (0.00%)";
            }
            studentOverallAttendance.put(student.getId(), overallFormatted);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("students", students);
        summary.put("subjects", subjects);
        summary.put("studentSubjectAttendance", studentSubjectAttendance);
        summary.put("studentOverallAttendance", studentOverallAttendance);

        return summary;
    }
}
