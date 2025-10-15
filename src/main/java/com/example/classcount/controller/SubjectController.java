package com.example.classcount.controller;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Subject;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.SubjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;

    public SubjectController(SubjectRepository subjectRepository, ClassroomRepository classroomRepository) {
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
    }

    // Display the list of subjects for a specific year
    @GetMapping("/{year}")
    public String listSubjects(@PathVariable("year") String year, Model model) {
        List<Subject> subjects = subjectRepository.findByClassroom_Year(year);
        model.addAttribute("subjects", subjects);
        model.addAttribute("currentYear", year);
        model.addAttribute("newSubject", new Subject()); // For the Add Subject form
        return "subject-list";
    }

    // Handle the form submission for adding or updating a subject
    @PostMapping("/add")
    public String addSubject(@ModelAttribute("newSubject") Subject subject, RedirectAttributes redirectAttributes) {
        try {
            // Find the classroom by year
            Classroom classroom = classroomRepository.findByYear(subject.getClassroom().getYear())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid year provided."));
            subject.setClassroom(classroom);
            subjectRepository.save(subject);
            redirectAttributes.addFlashAttribute("successMessage", "Subject saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving subject: " + e.getMessage());
        }
        return "redirect:/subjects/" + subject.getClassroom().getYear();
    }

    // Handle the form for editing a subject
    @GetMapping("/edit/{id}")
    public String editSubject(@PathVariable("id") Long id, Model model) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject ID:" + id));
        model.addAttribute("subject", subject);
        model.addAttribute("currentYear", subject.getClassroom().getYear());
        return "subject-edit";
    }

    // Handle the submission of the edited subject
    @PostMapping("/update/{id}")
    public String updateSubject(@PathVariable("id") Long id, @ModelAttribute("subject") Subject subject, RedirectAttributes redirectAttributes) {
        subject.setId(id);

        Classroom existingClassroom = classroomRepository.findById(subject.getClassroom().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid classroom ID."));
        subject.setClassroom(existingClassroom);

        subjectRepository.save(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Subject updated successfully!");

        // Correct redirect back to the subject list for the correct year
        return "redirect:/subjects/" + subject.getClassroom().getYear();
    }

    // Handle subject deletion
    @GetMapping("/delete/{id}")
    public String deleteSubject(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject ID:" + id));
        String year = subject.getClassroom().getYear();
        subjectRepository.delete(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully!");
        return "redirect:/subjects/" + year;
    }
}
