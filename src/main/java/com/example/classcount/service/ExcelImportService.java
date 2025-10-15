package com.example.classcount.service;

import com.example.classcount.entity.Classroom;
import com.example.classcount.entity.Student;
import com.example.classcount.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    private final StudentRepository studentRepository;

    public ExcelImportService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Processes the uploaded Excel file, extracts student data, and saves new Student entities.
     * Assumes Excel format is: Column 0 (Roll Number), Column 1 (Student Name).
     * @param file The uploaded .xlsx file.
     * @param classroom The Classroom entity (Year) to link the students to.
     * @return The count of students successfully imported.
     */
    public int importStudents(MultipartFile file, Classroom classroom) { // REMOVED section parameter
        List<Student> students = new ArrayList<>();
        int savedCount = 0;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            // Iterate over every row, starting from the second row (skipping headers)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);

                if (currentRow == null || currentRow.getCell(0) == null) continue;

                // Column 0 (A): Roll Number
                Cell rollNumberCell = currentRow.getCell(0);
                String rollNumber = "";

                if (rollNumberCell.getCellType() == CellType.STRING) {
                    rollNumber = rollNumberCell.getStringCellValue();
                } else if (rollNumberCell.getCellType() == CellType.NUMERIC) {
                    rollNumber = String.valueOf((long) rollNumberCell.getNumericCellValue());
                } else {
                    continue;
                }

                // Column 1 (B): Student Name
                Cell nameCell = currentRow.getCell(1);
                String studentName = nameCell.getStringCellValue();

                // Basic validation and object creation
                if (!studentName.isBlank() && !rollNumber.isBlank()) {
                    Student student = new Student();
                    student.setRollNumber(rollNumber);
                    student.setName(studentName);
                    student.setClassroom(classroom);
                    // NOTE: The student entity should no longer have setSection()
                    students.add(student);
                }
            }

            studentRepository.saveAll(students);
            savedCount = students.size();

        } catch (Exception e) {
            throw new RuntimeException("Failed to import Excel data: " + e.getMessage());
        }

        return savedCount;
    }
}
