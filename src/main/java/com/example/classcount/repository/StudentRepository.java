package com.example.classcount.repository;

import com.example.classcount.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // REVERTED: Finds all students belonging to a specific Classroom Year.
    List<Student> findByClassroom_Year(String year);

    // REVERTED: Finds a specific student by Roll Number and Year.
    Optional<Student> findByRollNumberAndClassroom_Year(String rollNumber, String year);
}
