package com.example.classcount;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import com.example.classcount.entity.User;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.StudentRepository;
import com.example.classcount.repository.SubjectRepository;
import com.example.classcount.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder, ClassroomRepository classroomRepository, StudentRepository studentRepository, SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.classroomRepository = classroomRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create an admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            // **CONFIRMING USE OF INJECTED encoder bean**
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setRole("ADMIN"); // Ensure ROLE_ prefix is present
            userRepository.save(adminUser);
            System.out.println("Default admin user created.");

            // Create classrooms if not exists
            if (classroomRepository.findByYear("1st Year").isEmpty()) {
                Classroom c1 = new Classroom();
                c1.setYear("1st Year");
                classroomRepository.save(c1);

                Classroom c2 = new Classroom();
                c2.setYear("2nd Year");
                classroomRepository.save(c2);

                Classroom c3 = new Classroom();
                c3.setYear("3rd Year");
                classroomRepository.save(c3);

                Classroom c4 = new Classroom();
                c4.setYear("4th Year");
                classroomRepository.save(c4);
            }



            
        }
    }
}


