package com.stemlink.skillmentor.Repositories;

import com.stemlink.skillmentor.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMentorId(Long mentorId);
    Optional<Review> findBySessionId(Long sessionId);
}
