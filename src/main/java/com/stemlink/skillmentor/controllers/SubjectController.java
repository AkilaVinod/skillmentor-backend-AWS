package com.stemlink.skillmentor.controllers;

import com.stemlink.skillmentor.Repositories.SessionRepository;
import com.stemlink.skillmentor.dto.request.SubjectRequestDTO;
import com.stemlink.skillmentor.dto.response.SubjectResponseDTO;
import com.stemlink.skillmentor.entities.Subject;
import com.stemlink.skillmentor.services.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/subjects")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubjectController extends AbstractController {

    private final ModelMapper modelMapper;
    private final SubjectService subjectService;
    private final SessionRepository sessionRepository;

    @GetMapping
    public ResponseEntity<Page<SubjectResponseDTO>> getAllSubjects(Pageable pageable) {

        Page<Subject> subjects = subjectService.getAllSubjects(pageable);

        Page<SubjectResponseDTO> response = subjects.map(subject -> {
            SubjectResponseDTO dto = modelMapper.map(subject, SubjectResponseDTO.class);
            dto.setMentorId(subject.getMentor().getMentorId());
            dto.setMentorName(subject.getMentor().getFirstName() + " " + subject.getMentor().getLastName());
            dto.setEnrollmentCount(sessionRepository.countBySubjectId(subject.getId()));
            return dto;
        });

        return sendOkResponse(response);
    }

    @GetMapping("{id}")
    public ResponseEntity<SubjectResponseDTO> getSubjectById(@PathVariable Long id) {
        Subject subject = subjectService.getSubjectById(id);
        SubjectResponseDTO subjectResponseDTO = modelMapper.map(subject, SubjectResponseDTO.class);
        subjectResponseDTO.setMentorId(subject.getMentor().getMentorId());
        subjectResponseDTO.setMentorName(subject.getMentor().getFirstName() + " " + subject.getMentor().getLastName());
        subjectResponseDTO.setEnrollmentCount(sessionRepository.countBySubjectId(subject.getId()));
        return sendOkResponse(subjectResponseDTO);
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<SubjectResponseDTO>> getSubjectsByMentor(@PathVariable String mentorId) {
        List<Subject> subjects = subjectService.getSubjectsByMentor(mentorId);
        List<SubjectResponseDTO> response = subjects.stream().map(subject -> {
            SubjectResponseDTO subjectResponseDTO = modelMapper.map(subject, SubjectResponseDTO.class);
            subjectResponseDTO.setMentorId(subject.getMentor().getMentorId());
            subjectResponseDTO.setMentorName(subject.getMentor().getFirstName() + " " + subject.getMentor().getLastName());
            subjectResponseDTO.setEnrollmentCount(sessionRepository.countBySubjectId(subject.getId()));
            return subjectResponseDTO;
        }).toList();
        return sendOkResponse(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponseDTO> createSubject(@Valid @RequestBody SubjectRequestDTO subjectDTO) {
        Subject subject = modelMapper.map(subjectDTO, Subject.class);
        Subject created = subjectService.addNewSubject(subjectDTO.getMentorId(), subject);

        SubjectResponseDTO subjectResponseDTO = modelMapper.map(created, SubjectResponseDTO.class);
        subjectResponseDTO.setMentorId(created.getMentor().getMentorId());
        subjectResponseDTO.setMentorName(created.getMentor().getFirstName() + " " + created.getMentor().getLastName());
        subjectResponseDTO.setEnrollmentCount(sessionRepository.countBySubjectId(created.getId()));
        return sendCreatedResponse(subjectResponseDTO);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponseDTO> updateSubject(@PathVariable Long id, @RequestBody SubjectRequestDTO updatedSubjectDTO) {
        Subject subject = modelMapper.map(updatedSubjectDTO, Subject.class);
        Subject updated = subjectService.updateSubjectById(id, subject);

        SubjectResponseDTO subjectResponseDTO = modelMapper.map(updated, SubjectResponseDTO.class);
        subjectResponseDTO.setMentorId(updated.getMentor().getMentorId());
        subjectResponseDTO.setMentorName(updated.getMentor().getFirstName() + " " + updated.getMentor().getLastName());
        subjectResponseDTO.setEnrollmentCount(sessionRepository.countBySubjectId(updated.getId()));
        return sendOkResponse(subjectResponseDTO);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return sendNoContentResponse();
    }

}