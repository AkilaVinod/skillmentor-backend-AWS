package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.dto.request.StudentRequestDTO;
import com.stemlink.skillmentor.dto.response.StudentResponseDTO;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.services.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.stemlink.skillmentor.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static com.stemlink.skillmentor.constants.UserRoles.*;

@RestController
@RequestMapping(path = "/api/v1/students")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class StudentController extends AbstractController{

    private final StudentService studentService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {

        List<Student> students = studentService.getAllStudents();

        List<StudentResponseDTO> response = students.stream()
                .map(student -> modelMapper.map(student, StudentResponseDTO.class))
                .toList();

        return sendOkResponse(response);
    }

    @GetMapping("{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {

        Student student = studentService.getStudentById(id);

        StudentResponseDTO response = modelMapper.map(student, StudentResponseDTO.class);

        return sendOkResponse(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('" + ROLE_STUDENT + "')")
    public ResponseEntity<StudentResponseDTO> getCurrentStudent(Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Student student = studentService.getStudentByStudentId(userPrincipal.getId());

        StudentResponseDTO response = modelMapper.map(student, StudentResponseDTO.class);

        return sendOkResponse(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_STUDENT + "')")
    public ResponseEntity<StudentResponseDTO> createStudent(
            @Valid @RequestBody StudentRequestDTO studentDTO,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Student student = modelMapper.map(studentDTO, Student.class);

        student.setStudentId(userPrincipal.getId());
        student.setFirstName(userPrincipal.getFirstName());
        student.setLastName(userPrincipal.getLastName());
        student.setEmail(userPrincipal.getEmail());

        Student createdStudent = studentService.createNewStudent(student);

        StudentResponseDTO response =
                modelMapper.map(createdStudent, StudentResponseDTO.class);

        return sendCreatedResponse(response);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_STUDENT + "')")
    public ResponseEntity<StudentResponseDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO updatedStudentDTO,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !userPrincipal.getId().equals(studentService.getStudentById(id).getStudentId())) {
            throw new AccessDeniedException("You cannot update another student");
        }

        Student student = modelMapper.map(updatedStudentDTO, Student.class);

        Student updatedStudent = studentService.updateStudentById(id, student);

        StudentResponseDTO response =
                modelMapper.map(updatedStudent, StudentResponseDTO.class);

        return sendOkResponse(response);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole('" + ROLE_ADMIN + "')")
        public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return sendNoContentResponse();
    }
}

