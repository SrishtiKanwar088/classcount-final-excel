# TODO: Remove Manual Add Student and Add Excel Import

## Steps to Complete
- [x] Create ExcelImportService.java: Implement service to read Excel file, parse students (Name, Roll Number), save to DB for given year, skip duplicates.
- [x] Update StudentController.java: Remove addStudent POST method, add importStudents POST method to handle file upload and call import service.
- [x] Update student-list.html: Remove "Add New Student" form, replace with "Import Students from Excel" file upload form.
- [ ] Test the import functionality with a sample Excel file.
