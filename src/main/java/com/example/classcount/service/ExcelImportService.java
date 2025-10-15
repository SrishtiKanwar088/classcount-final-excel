package com.example.classcount.service;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.repository.ClassroomRepository;
import com.example.classcount.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExcelImportService {

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;

    public ExcelImportService(StudentRepository studentRepository, ClassroomRepository classroomRepository) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
    }

    /**
     * Helper method to safely read a string value from a cell,
     * handling null cells and numeric cell types.
     */
    private Optional<String> getCellValueAsString(Cell cell) {
        if (cell == null) {
            return Optional.empty();
        }

        try {
            if (cell.getCellType() == CellType.STRING) {
                return Optional.of(cell.getStringCellValue().trim());
            } else if (cell.getCellType() == CellType.NUMERIC) {
                double number = cell.getNumericCellValue();
                if (number == Math.floor(number)) {
                    return Optional.of(String.valueOf((int) number));
                } else {
                    return Optional.of(String.valueOf(number));
                }
            } else if (cell.getCellType() == CellType.BLANK) {
                return Optional.empty();
            }
        } catch (Exception e) {
            // Log or handle conversion errors if necessary
        }
        return Optional.empty();
    }

    public void importStudentsFromExcel(MultipartFile file, String year) throws IOException {
        // 1. Get the target Classroom
        Classroom classroom = classroomRepository.findByYear(year)
                .orElseThrow(() -> new IllegalArgumentException("Invalid year provided: " + year));

        List<Student> studentsToSave = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Assume first sheet

            // Iterate over all rows
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                // Columns: 0: Name, 1: Roll Number, 2: Section
                Cell nameCell = row.getCell(0);
                Cell rollNumberCell = row.getCell(1);
                Cell sectionCell = row.getCell(2);

                // Safely extract values
                Optional<String> optName = getCellValueAsString(nameCell);
                Optional<String> optRollNumber = getCellValueAsString(rollNumberCell);
                Optional<String> optSection = getCellValueAsString(sectionCell);

                // All three values (Name, Roll Number, Section) must be present and non-empty.
                if (optName.isPresent() && optRollNumber.isPresent() && optSection.isPresent()) {
                    String name = optName.get();
                    String rollNumber = optRollNumber.get();
                    String section = optSection.get();

                    // 🔥 CRITICAL CHANGE: Check for duplicate roll number ONLY within the specified year
                    if (studentRepository.findByRollNumberAndClassroom_Year(rollNumber, year).isEmpty()) {
                        Student student = new Student();
                        student.setName(name);
                        student.setRollNumber(rollNumber);
                        student.setSection(section);
                        student.setClassroom(classroom);
                        studentsToSave.add(student);
                    } else {
                        // Optional: Log which row was skipped due to duplicate roll number
                        System.out.println("Skipping row " + (row.getRowNum() + 1) + ": Duplicate Roll Number found in this year: " + rollNumber);
                    }
                } else {
                    // Optional: Log which row was skipped due to missing data
                    System.out.println("Skipping row " + (row.getRowNum() + 1) + ": Missing Name, Roll Number, or Section.");
                }
            }
        }

        // 2. Save all valid students
        if (!studentsToSave.isEmpty()) {
            studentRepository.saveAll(studentsToSave);
        }
    }
}