package com.example.classcount.repository;

import com.example.classcount.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByClassroom_Year(String year);

    // NEW METHOD: Find a specific student by Roll Number within a Classroom
    Optional<Student> findByRollNumberAndClassroom_Year(String rollNumber, String year);

    Optional<Student> findByRollNumber(String rollNumber);

}