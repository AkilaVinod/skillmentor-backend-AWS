package com.stemlink.skillmentor.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class SubjectResponseDTO {

    private Long id;

    private String subjectName;
    private String description;
    private String courseImageUrl;

    private String mentorId;
    private String mentorName;   // combine first + last

    private Long enrollmentCount;

    private Date createdAt;
    private Date updatedAt;
}
