package com.stemlink.skillmentor.Repositories;

import com.stemlink.skillmentor.entities.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {

    List<MentorAvailability> findByMentorIdOrderByDayOfWeekAscStartTimeAsc(Long mentorId);
}
