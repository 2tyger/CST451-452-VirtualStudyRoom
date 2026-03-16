/*
defines task request and response contracts used by room task operations
*/
package com.tygilbert.virtualstudyroom.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(min = 2, max = 200) String title,
        @Size(max = 1000) String description
) {
}

