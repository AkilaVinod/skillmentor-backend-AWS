package com.stemlink.skillmentor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentRequestDTO {

        @Size(max = 1000, message = "Learning goals too long")
        private String learningGoals;

}
