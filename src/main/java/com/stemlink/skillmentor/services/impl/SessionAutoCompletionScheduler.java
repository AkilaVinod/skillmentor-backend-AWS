package com.stemlink.skillmentor.services.impl;

import com.stemlink.skillmentor.Repositories.SessionRepository;
import com.stemlink.skillmentor.constants.SessionStatus;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionAutoCompletionScheduler {

    private final SessionRepository sessionRepository;

    /**
     * Periodically mark sessions as COMPLETED once their end time has passed.
     * Runs every 10 minutes.
     */
    @Scheduled(fixedDelayString = "600000")
    public void autoCompleteFinishedSessions() {
        List<Session> confirmedSessions = sessionRepository.findBySessionStatus(SessionStatus.CONFIRMED);
        if (confirmedSessions.isEmpty()) {
            return;
        }

        Date now = new Date();
        List<Session> toComplete = new ArrayList<>();

        for (Session session : confirmedSessions) {
            if (session.getSessionAt() == null) {
                continue;
            }

            int durationMinutes = ValidationUtils.normalizeDuration(session.getDurationMinutes());
            Date endTime = ValidationUtils.addMinutesToDate(session.getSessionAt(), durationMinutes);

            if (!endTime.after(now)) {
                session.setSessionStatus(SessionStatus.COMPLETED);
                toComplete.add(session);
            }
        }

        if (!toComplete.isEmpty()) {
            log.info("Auto-completing {} sessions whose end time has passed", toComplete.size());
            sessionRepository.saveAll(toComplete);
        }
    }
}

