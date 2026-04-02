package com.stemlink.skillmentor.configs;

import com.stemlink.skillmentor.Repositories.*;
import com.stemlink.skillmentor.constants.AppUserRole;
import com.stemlink.skillmentor.constants.PaymentStatus;
import com.stemlink.skillmentor.constants.SessionStatus;
import com.stemlink.skillmentor.entities.*;
import com.stemlink.skillmentor.utils.MentorAvailabilityUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    public CommandLineRunner loadDemoData(
            MentorRepository mentorRepository,
            SubjectRepository subjectRepository,
            StudentRepository studentRepository,
            SessionRepository sessionRepository,
            ReviewRepository reviewRepository,
            UserProfileRepository userProfileRepository
    ) {
        return args -> {
            // Avoid reseeding if data already exists
            if (mentorRepository.count() > 0 || studentRepository.count() > 0) {
                return;
            }

            // ---- Mentors ----
            Mentor mentorAlice = new Mentor();
            mentorAlice.setMentorId("M-ALICE-001");
            mentorAlice.setFirstName("Alice");
            mentorAlice.setLastName("Johnson");
            mentorAlice.setEmail("alice.mentor@example.com");
            mentorAlice.setPhoneNumber("+1-555-0101");
            mentorAlice.setTitle("Senior Software Engineer");
            mentorAlice.setProfession("Backend Engineering");
            mentorAlice.setCompany("TechNova Inc.");
            mentorAlice.setExperienceYears(8);
            mentorAlice.setBio("Specialized in Java, Spring Boot and distributed systems. Passionate about mentoring early-career engineers.");
            mentorAlice.setProfileImageUrl("https://images.pexels.com/photos/1181695/pexels-photo-1181695.jpeg");
            mentorAlice.setAverageRating(4.8);
            mentorAlice.setTotalReviews(5);
            mentorAlice.setTotalEnrollments(40);
            mentorAlice.setIsCertified(true);
            mentorAlice.setStartYear("2017");
            mentorAlice.setAvailabilities(MentorAvailabilityUtils.buildDefaultWeekdayAvailability(mentorAlice));

            Mentor mentorBob = new Mentor();
            mentorBob.setMentorId("M-BOB-002");
            mentorBob.setFirstName("Bob");
            mentorBob.setLastName("Singh");
            mentorBob.setEmail("bob.mentor@example.com");
            mentorBob.setPhoneNumber("+1-555-0102");
            mentorBob.setTitle("Staff Frontend Engineer");
            mentorBob.setProfession("Frontend Engineering");
            mentorBob.setCompany("PixelCraft Labs");
            mentorBob.setExperienceYears(10);
            mentorBob.setBio("Helps engineers level up in React, TypeScript and modern frontend architecture.");
            mentorBob.setProfileImageUrl("https://images.pexels.com/photos/2379005/pexels-photo-2379005.jpeg");
            mentorBob.setAverageRating(4.9);
            mentorBob.setTotalReviews(7);
            mentorBob.setTotalEnrollments(55);
            mentorBob.setIsCertified(true);
            mentorBob.setStartYear("2015");
            mentorBob.setAvailabilities(MentorAvailabilityUtils.buildDefaultWeekdayAvailability(mentorBob));

            mentorRepository.saveAll(List.of(mentorAlice, mentorBob));

            // ---- Subjects ----
            Subject javaSubject = new Subject();
            javaSubject.setSubjectName("Practical Java & Spring Boot");
            javaSubject.setDescription("Hands-on coaching for building REST APIs with Spring Boot, JPA and PostgreSQL.");
            javaSubject.setCourseImageUrl("https://images.pexels.com/photos/1181671/pexels-photo-1181671.jpeg");
            javaSubject.setMentor(mentorAlice);

            Subject systemDesign = new Subject();
            systemDesign.setSubjectName("System Design for Interviews");
            systemDesign.setDescription("Design scalable systems, prepare for FAANG-style interviews with real-world scenarios.");
            systemDesign.setCourseImageUrl("https://images.pexels.com/photos/3861958/pexels-photo-3861958.jpeg");
            systemDesign.setMentor(mentorAlice);

            Subject reactSubject = new Subject();
            reactSubject.setSubjectName("Modern React & TypeScript");
            reactSubject.setDescription("From fundamentals to advanced patterns: hooks, state management, performance and testing.");
            reactSubject.setCourseImageUrl("https://images.pexels.com/photos/1181675/pexels-photo-1181675.jpeg");
            reactSubject.setMentor(mentorBob);

            Subject uiUx = new Subject();
            uiUx.setSubjectName("Frontend Architecture & UX");
            uiUx.setDescription("Architect large-scale frontends with clean design systems and great user experience.");
            uiUx.setCourseImageUrl("https://images.pexels.com/photos/196645/pexels-photo-196645.jpeg");
            uiUx.setMentor(mentorBob);

            subjectRepository.saveAll(List.of(javaSubject, systemDesign, reactSubject, uiUx));

            // ---- Students ----
            Student studentEmily = new Student();
            studentEmily.setStudentId("S-EMILY-001");
            studentEmily.setEmail("emily.student@example.com");
            studentEmily.setFirstName("Emily");
            studentEmily.setLastName("Brown");
            studentEmily.setLearningGoals("Transition from QA to backend engineering within 6 months.");

            Student studentLiam = new Student();
            studentLiam.setStudentId("S-LIAM-002");
            studentLiam.setEmail("liam.student@example.com");
            studentLiam.setFirstName("Liam");
            studentLiam.setLastName("Chen");
            studentLiam.setLearningGoals("Become a senior frontend engineer and lead a small team.");

            studentRepository.saveAll(List.of(studentEmily, studentLiam));

            // ---- Sessions ----
            Date nextWeek = Date.from(LocalDateTime.now().plusDays(7)
                    .atZone(ZoneId.systemDefault()).toInstant());
            Date tomorrow = Date.from(LocalDateTime.now().plusDays(1)
                    .atZone(ZoneId.systemDefault()).toInstant());
            Date lastWeek = Date.from(LocalDateTime.now().minusDays(7)
                    .atZone(ZoneId.systemDefault()).toInstant());

            Session emilyJavaSession = new Session();
            emilyJavaSession.setStudent(studentEmily);
            emilyJavaSession.setMentor(mentorAlice);
            emilyJavaSession.setSubject(javaSubject);
            emilyJavaSession.setSessionAt(tomorrow);
            emilyJavaSession.setDurationMinutes(60);
            emilyJavaSession.setSessionStatus(SessionStatus.CONFIRMED);
            emilyJavaSession.setMeetingLink("https://meet.example.com/emily-java");
            emilyJavaSession.setSessionNotes("Intro session: assess current Java knowledge and define roadmap.");
            emilyJavaSession.setPaymentStatus(PaymentStatus.PAID);

            Session emilySystemDesign = new Session();
            emilySystemDesign.setStudent(studentEmily);
            emilySystemDesign.setMentor(mentorAlice);
            emilySystemDesign.setSubject(systemDesign);
            emilySystemDesign.setSessionAt(nextWeek);
            emilySystemDesign.setDurationMinutes(90);
            emilySystemDesign.setSessionStatus(SessionStatus.PENDING);
            emilySystemDesign.setMeetingLink("https://meet.example.com/emily-system-design");
            emilySystemDesign.setSessionNotes("Deep dive into system design basics.");
            emilySystemDesign.setPaymentStatus(PaymentStatus.PENDING);

            Session liamReactSession = new Session();
            liamReactSession.setStudent(studentLiam);
            liamReactSession.setMentor(mentorBob);
            liamReactSession.setSubject(reactSubject);
            liamReactSession.setSessionAt(lastWeek);
            liamReactSession.setDurationMinutes(60);
            liamReactSession.setSessionStatus(SessionStatus.COMPLETED);
            liamReactSession.setMeetingLink("https://meet.example.com/liam-react");
            liamReactSession.setSessionNotes("Covered hooks best practices and performance optimization.");
            liamReactSession.setPaymentStatus(PaymentStatus.PAID);

            sessionRepository.saveAll(List.of(emilyJavaSession, emilySystemDesign, liamReactSession));

            // ---- Reviews ----
            Review reviewForAlice = Review.builder()
                    .rating(5)
                    .comment("Alice explained complex backend concepts in a very approachable way.")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .mentor(mentorAlice)
                    .student(studentEmily)
                    .build();

            Review reviewForBob = Review.builder()
                    .rating(5)
                    .comment("Bob helped me structure a large React codebase and improved my confidence.")
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .mentor(mentorBob)
                    .student(studentLiam)
                    .session(liamReactSession)
                    .build();

            reviewRepository.saveAll(List.of(reviewForAlice, reviewForBob));

            // ---- User Profiles (for role-based UX) ----
            UserProfile emilyProfile = new UserProfile();
            emilyProfile.setClerkUserId("clerk_emily_demo");
            emilyProfile.setEmail(studentEmily.getEmail());
            emilyProfile.setFirstName(studentEmily.getFirstName());
            emilyProfile.setLastName(studentEmily.getLastName());
            emilyProfile.setRole(AppUserRole.STUDENT);

            UserProfile liamProfile = new UserProfile();
            liamProfile.setClerkUserId("clerk_liam_demo");
            liamProfile.setEmail(studentLiam.getEmail());
            liamProfile.setFirstName(studentLiam.getFirstName());
            liamProfile.setLastName(studentLiam.getLastName());
            liamProfile.setRole(AppUserRole.STUDENT);

            UserProfile aliceProfile = new UserProfile();
            aliceProfile.setClerkUserId("clerk_alice_demo");
            aliceProfile.setEmail(mentorAlice.getEmail());
            aliceProfile.setFirstName(mentorAlice.getFirstName());
            aliceProfile.setLastName(mentorAlice.getLastName());
            aliceProfile.setRole(AppUserRole.MENTOR);

            UserProfile adminProfile = new UserProfile();
            adminProfile.setClerkUserId("clerk_admin_demo");
            adminProfile.setEmail("admin@example.com");
            adminProfile.setFirstName("Admin");
            adminProfile.setLastName("User");
            adminProfile.setRole(AppUserRole.ADMIN);

            userProfileRepository.saveAll(List.of(emilyProfile, liamProfile, aliceProfile, adminProfile));
        };
    }
}

