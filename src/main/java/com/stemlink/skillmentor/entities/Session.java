package com.stemlink.skillmentor.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stemlink.skillmentor.constants.PaymentStatus;
import com.stemlink.skillmentor.constants.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "session")
@Data
public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne
    @JoinColumn(name = "mentor_id", nullable = false)
    @JsonIgnore
    private Mentor mentor;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnore
    private Subject subject;

    @Column(name = "session_at", nullable = false)
    private Date sessionAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    private SessionStatus sessionStatus;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "session_notes", columnDefinition = "TEXT")
    private String sessionNotes;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 150)
    private String paymentReference;

    @Column(name = "payment_notes", columnDefinition = "TEXT")
    private String paymentNotes;

    @Column(name = "payment_proof_path")
    private String paymentProofPath;

    @Column(name = "payment_proof_file_name", length = 255)
    private String paymentProofFileName;

    @Column(name = "payment_submitted_at")
    private Date paymentSubmittedAt;

    @OneToOne(mappedBy = "session")
    @JsonIgnore
    private Review review;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;
}
