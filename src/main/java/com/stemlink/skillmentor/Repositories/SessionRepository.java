package com.stemlink.skillmentor.Repositories;

import com.stemlink.skillmentor.constants.SessionStatus;
import com.stemlink.skillmentor.entities.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session,Long>, JpaSpecificationExecutor<Session> {

    List<Session> findByStudent_EmailOrderBySessionAtDesc(String email);

    List<Session> findByStudent_StudentIdOrderBySessionAtDesc(String studentId);

    long countBySubjectId(Long subjectId);

    long countByMentorId(Long mentorId);

    @Query("SELECT COUNT(DISTINCT s.student.id) FROM Session s WHERE s.mentor.id = :mentorId")
    long countDistinctStudentsByMentorId(@Param("mentorId") Long mentorId);

    List<Session> findByMentorIdAndSessionStatusIn(Long mentorId, Collection<SessionStatus> statuses);

    List<Session> findByStudentIdAndSessionStatusIn(Long studentId, Collection<SessionStatus> statuses);

    List<Session> findBySessionStatus(SessionStatus status);

}
