package com.stemlink.skillmentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StudentResponseDTO {

    private Integer id;

    private String studentId;
    private String email;
    private String firstName;
    private String lastName;
    private String learningGoals;

    private Date createdAt;
    private Date updatedAt;
}
