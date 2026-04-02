package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.MentorRepository;
import com.stemlink.skillmentor.Repositories.SubjectRepository;
import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.Subject;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import com.stemlink.skillmentor.services.SubjectService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final MentorRepository mentorRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "subjects", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Subject> getAllSubjects(Pageable pageable) {
        try {
            log.info("Fetching subjects page {} from DB", pageable.getPageNumber());
            return subjectRepository.findAll(pageable);
        } catch (Exception exception) {
            log.error("Failed to get all subjects", exception);
            throw new SkillMentorException("Failed to get all subjects", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Cacheable(value = "subjectsByMentor", key = "#mentorId")
    public List<Subject> getSubjectsByMentor(String mentorId) {
        log.info("Fetching subjects for mentor {} from DB", mentorId);
        return subjectRepository.findByMentor_MentorId(mentorId);
    }

    @Override
    @CacheEvict(value = {"subjects", "subjectsByMentor"}, allEntries = true)
    public Subject addNewSubject(String mentorId, Subject subject) {
        try {
            Mentor mentor = mentorRepository.findByMentorId(mentorId)
                    .orElseThrow(() -> new SkillMentorException("Mentor not found", HttpStatus.NOT_FOUND));

            subject.setMentor(mentor);

            return subjectRepository.save(subject);

        } catch (SkillMentorException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding subject: {}", e.getMessage());
            throw new SkillMentorException("Subject already exists or database constraint violation", HttpStatus.CONFLICT);
        } catch (Exception exception) {
            log.error("Failed to add new subject", exception);
            throw new SkillMentorException("Failed to add new subject", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Cacheable(value = "subject", key = "#id")
    public Subject getSubjectById(Long id) {
        log.info("Fetching subject {} from DB", id);
        return subjectRepository.findById(id).orElseThrow(
                () -> new SkillMentorException("Subject not found", HttpStatus.NOT_FOUND)
        );
    }

    @Override
    @CacheEvict(value = {"subjects", "subjectsByMentor", "subject"}, allEntries = true)
    public Subject updateSubjectById(Long id, Subject updatedSubject) {
        try {
            Subject subject = subjectRepository.findById(id).orElseThrow(
                    () -> new SkillMentorException("Subject not found", HttpStatus.NOT_FOUND)
            );

            modelMapper.map(updatedSubject, subject);

            return subjectRepository.save(subject);

        } catch (SkillMentorException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating subject: {}", e.getMessage());
            throw new SkillMentorException("Database constraint violation", HttpStatus.CONFLICT);
        } catch (Exception exception) {
            log.error("Error updating subject", exception);
            throw new SkillMentorException("Failed to update subject", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @CacheEvict(value = {"subjects", "subjectsByMentor", "subject"}, allEntries = true)
    public void deleteSubject(Long id) {
        try {
            Subject subject = subjectRepository.findById(id).orElseThrow(
                    () -> new SkillMentorException("Subject not found", HttpStatus.NOT_FOUND));

            subjectRepository.delete(subject);

        } catch (Exception exception) {
            log.error("Failed to delete subject with id {}", id, exception);
            throw new SkillMentorException("Failed to delete subject", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}