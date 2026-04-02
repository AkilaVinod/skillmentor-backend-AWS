package com.stemlink.skillmentor.Repositories;

import com.stemlink.skillmentor.entities.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject,Long> {
    List<Subject> findByMentor_MentorId(String mentorId);
}
