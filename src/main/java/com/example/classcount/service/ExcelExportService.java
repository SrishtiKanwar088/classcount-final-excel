package com.example.classcount.service;

import com.example.classcount.entity.Attendance;
import com.example.classcount.entity.Student;
import com.example.classcount.entity.Subject;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    public void exportAttendance(HttpServletResponse response, List<Student> students, List<Subject> subjects, List<Attendance> allAttendance) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        // Create header row
        Row headerRow = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Cell cell = headerRow.createCell(0);
        cell.setCellValue("Student Name");
        cell.setCellStyle(headerStyle);

        int cellIndex = 1;
        for (Subject subject : subjects) {
            Cell subjectCell = headerRow.createCell(cellIndex++);
            subjectCell.setCellValue(subject.getName());
            subjectCell.setCellStyle(headerStyle);
        }
        Cell overallCell = headerRow.createCell(cellIndex);
        overallCell.setCellValue("Overall %");
        overallCell.setCellStyle(headerStyle);

        // Map for quick lookup of attendance data
        Map<Long, Map<Long, Integer>> studentSubjectPresent = new HashMap<>(); // <studentId, <subjectId, presentCount>>
        Map<Long, Map<Long, Integer>> studentSubjectTotal = new HashMap<>(); // <studentId, <subjectId, totalCount>>
        Map<Long, Integer> studentOverallPresent = new HashMap<>();
        Map<Long, Integer> studentOverallTotal = new HashMap<>();

        for (Attendance att : allAttendance) {
            Long studentId = att.getStudent().getId();
            Long subjectId = att.getSubject().getId();

            studentSubjectTotal.computeIfAbsent(studentId, k -> new HashMap<>()).merge(subjectId, 1, Integer::sum);
            studentOverallTotal.merge(studentId, 1, Integer::sum);

            if (att.isPresent()) {
                studentSubjectPresent.computeIfAbsent(studentId, k -> new HashMap<>()).merge(subjectId, 1, Integer::sum);
                studentOverallPresent.merge(studentId, 1, Integer::sum);
            }
        }

        // Fill data rows
        int rowIndex = 1;
        for (Student student : students) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(student.getName());

            cellIndex = 1;
            for (Subject subject : subjects) {
                int present = studentSubjectPresent.getOrDefault(student.getId(), new HashMap<>()).getOrDefault(subject.getId(), 0);
                int total = studentSubjectTotal.getOrDefault(student.getId(), new HashMap<>()).getOrDefault(subject.getId(), 0);
                dataRow.createCell(cellIndex++).setCellValue(present + "/" + total);
            }

            int overallPresentCount = studentOverallPresent.getOrDefault(student.getId(), 0);
            int overallTotalCount = studentOverallTotal.getOrDefault(student.getId(), 0);
            String overallPercentage = "0.00%";
            if (overallTotalCount > 0) {
                double percentage = (double) overallPresentCount / overallTotalCount * 100;
                overallPercentage = String.format("%.2f", percentage) + "%";
            }
            dataRow.createCell(cellIndex).setCellValue(overallPercentage);
        }

        // Auto-size columns
        for(int i = 0; i < cellIndex + 1; i++){
            sheet.autoSizeColumn(i);
        }

        // Write the workbook to the response
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }
    }
}