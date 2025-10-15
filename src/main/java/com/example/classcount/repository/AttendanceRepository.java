package com.example.classcount.repository;

import com.example.classcount.entity.Attendance;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySubjectAndDate(Subject subject, LocalDate date);
    List<Attendance> findByStudentAndSubject(Student student, Subject subject);
    List<Attendance> findBySubject(Subject subject);

    /**
     * CRITICAL: Deletes all attendance records associated with a list of student IDs.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.student.id IN :studentIds")
    int deleteByStudentIdIn(List<Long> studentIds);
}
