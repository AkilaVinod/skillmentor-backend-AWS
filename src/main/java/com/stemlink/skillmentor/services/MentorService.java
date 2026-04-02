package com.stemlink.skillmentor.services;

import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.dto.request.MentorRequestDTO;
import com.stemlink.skillmentor.dto.response.MentorProvisionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface MentorService {
    Mentor createNewMentor(Mentor mentor);
    MentorProvisionResponseDTO provisionMentor(MentorRequestDTO mentorRequestDTO);
    Page<Mentor> getAllMentors(String name, Pageable pageable);
    Mentor getMentorById(Long id);
    Mentor updateMentorById(Long id, Mentor updatedMentor);
    void deleteMentor(Long id);
    Mentor getMentorByMentorId(String mentorId);
}

