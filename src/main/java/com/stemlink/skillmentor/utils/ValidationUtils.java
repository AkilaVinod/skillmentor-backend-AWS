package com.stemlink.skillmentor.utils;

import com.stemlink.skillmentor.entities.Mentor;
import com.stemlink.skillmentor.entities.MentorAvailability;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.entities.Student;
import com.stemlink.skillmentor.exceptions.SkillMentorException;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ValidationUtils {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0);

    public static int normalizeDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return 60;
        }

        return durationMinutes;
    }

    public static void validateSessionTime(Date sessionAt) {
        if (sessionAt == null) {
            throw new SkillMentorException("Session date/time cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (!sessionAt.after(new Date())) {
            throw new SkillMentorException("Session date cannot be in the past", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Validates if the mentor is available during the requested session time
     * @param mentor The mentor entity
     * @param sessionAt The session start time
     * @param durationMinutes The session duration in minutes
     * @throws IllegalArgumentException if mentor is not available
     */
    public static void validateMentorAvailability(
            Mentor mentor,
            List<MentorAvailability> availabilities,
            List<Session> mentorSessions,
            Date sessionAt,
            Integer durationMinutes,
            Long excludedSessionId
    ) {
        int normalizedDuration = normalizeDuration(durationMinutes);
        validateWithinAvailabilityWindow(mentor, availabilities, sessionAt, normalizedDuration);

        Date sessionEnd = addMinutesToDate(sessionAt, normalizedDuration);

        for (Session existingSession : mentorSessions) {
            if (excludedSessionId != null && Objects.equals(existingSession.getId(), excludedSessionId)) {
                continue;
            }

            Date existingStart = existingSession.getSessionAt();
            Date existingEnd = addMinutesToDate(existingStart, normalizeDuration(existingSession.getDurationMinutes()));

            if (isTimeOverlap(sessionAt, sessionEnd, existingStart, existingEnd)) {
                throw new SkillMentorException("Mentor is not available at the requested time", HttpStatus.CONFLICT);
            }
        }
    }

    /**
     * Validates if the student is available during the requested session time
     * @param student The student entity
     * @param sessionAt The session start time
     * @param durationMinutes The session duration in minutes
     * @throws IllegalArgumentException if student is not available
     */
    public static void validateStudentAvailability(
            Student student,
            List<Session> studentSessions,
            Long subjectId,
            Date sessionAt,
            Integer durationMinutes,
            Long excludedSessionId
    ) {
        int normalizedDuration = normalizeDuration(durationMinutes);
        Date sessionEnd = addMinutesToDate(sessionAt, normalizedDuration);

        for (Session existingSession : studentSessions) {
            if (excludedSessionId != null && Objects.equals(existingSession.getId(), excludedSessionId)) {
                continue;
            }

            Date existingStart = existingSession.getSessionAt();
            Date existingEnd = addMinutesToDate(existingStart, normalizeDuration(existingSession.getDurationMinutes()));

            if (isTimeOverlap(sessionAt, sessionEnd, existingStart, existingEnd)) {
                if (existingSession.getMentor() != null &&
                        existingSession.getSubject() != null &&
                        existingSession.getSubject().getId() != null &&
                        existingSession.getSubject().getId().equals(subjectId)) {
                    throw new SkillMentorException("You already have a session booked for this subject during that time window", HttpStatus.CONFLICT);
                }

                throw new SkillMentorException("You already have another session booked during that time window", HttpStatus.CONFLICT);
            }
        }
    }

    /**
     * Checks if two time periods overlap
     */
    public static boolean isTimeOverlap(Date start1, Date end1, Date start2, Date end2) {
        return start1.before(end2) && start2.before(end1);
    }

    /**
     * Adds minutes to a given date
     */
    public static Date addMinutesToDate(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private static void validateWithinAvailabilityWindow(
            Mentor mentor,
            List<MentorAvailability> availabilities,
            Date sessionAt,
            int durationMinutes
    ) {
        LocalDateTime start = LocalDateTime.ofInstant(sessionAt.toInstant(), SYSTEM_ZONE);
        LocalTime requestedStart = start.toLocalTime();
        LocalTime requestedEnd = requestedStart.plusMinutes(durationMinutes);
        DayOfWeek requestedDay = start.getDayOfWeek();

        boolean withinAvailability;

        if (availabilities == null || availabilities.isEmpty()) {
            withinAvailability = requestedDay != DayOfWeek.SATURDAY
                    && requestedDay != DayOfWeek.SUNDAY
                    && !requestedStart.isBefore(DEFAULT_START_TIME)
                    && !requestedEnd.isAfter(DEFAULT_END_TIME);
        } else {
            withinAvailability = availabilities.stream().anyMatch(availability ->
                    availability.getDayOfWeek() == requestedDay
                            && !requestedStart.isBefore(availability.getStartTime())
                            && !requestedEnd.isAfter(availability.getEndTime())
            );
        }

        if (!withinAvailability) {
            throw new SkillMentorException(
                    mentor.getFirstName() + " is not available at the requested time",
                    HttpStatus.CONFLICT
            );
        }
    }
}

