package com.stemlink.skillmentor.services;

import com.stemlink.skillmentor.entities.Student;
import java.util.List;

public interface StudentService {

    Student createNewStudent(Student student);
    List<Student> getAllStudents();
    Student getStudentById(Long id);
    Student getStudentByStudentId(String studentId);
    Student updateStudentById(Long id, Student updatedStudent);
    void deleteStudent(Long id);
}