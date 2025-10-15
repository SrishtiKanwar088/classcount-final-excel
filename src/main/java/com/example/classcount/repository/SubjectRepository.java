package com.example.classcount.repository;

import com.example.classcount.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    // Returns all subjects belonging to a particular classroom year
    List<Subject> findByClassroom_Year(String year);
}
