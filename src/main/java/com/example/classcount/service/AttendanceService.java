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
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;

    public AttendanceService(StudentRepository studentRepository, SubjectRepository subjectRepository, AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
    }

    // Method to get attendance summary for a given year
    public Map<String, Object> getAttendanceSummaryForYear(String year) {
        List<Student> students = studentRepository.findByClassroom_Year(year);
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);

        List<Attendance> allAttendance = attendanceRepository.findAll();
        List<Attendance> attendanceForYear = allAttendance.stream()
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