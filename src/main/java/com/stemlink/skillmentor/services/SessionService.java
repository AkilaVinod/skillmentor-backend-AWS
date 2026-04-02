package com.stemlink.skillmentor.services;


import com.stemlink.skillmentor.dto.request.SessionRequestDTO;
import com.stemlink.skillmentor.entities.Session;
import com.stemlink.skillmentor.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface SessionService {

    Session createNewSession(SessionRequestDTO sessionRequestDTO);

    Page<Session> getAllSessions(String status, String search, Date startDate, Date endDate, Pageable pageable);

    Session getSessionById(Long id);

    Session updateSessionById(Long id, SessionRequestDTO updatedSessionDTO);

    void deleteSession(Long id);

    Session enrollSession(UserPrincipal userPrincipal, SessionRequestDTO sessionRequestDTO);

    List<Session> getSessionsByStudent(UserPrincipal userPrincipal);

    Session uploadPaymentProof(Long id, UserPrincipal userPrincipal, MultipartFile file, String paymentMethod,
                               String paymentReference, String paymentNotes);

    Session confirmPayment(Long id);

    Session markComplete(Long id);

    Session addMeetingLink(Long id, String meetingLink);
}

