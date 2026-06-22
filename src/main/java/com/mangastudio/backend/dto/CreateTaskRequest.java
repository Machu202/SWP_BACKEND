package com.mangastudio.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateTaskRequest(
    @NotNull(message = "Missing assigneeId") Long assigneeId,
    @NotBlank(message = "Task description cannot be empty") String taskDesc,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double xNorm,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double yNorm,
    @NotNull @DecimalMin("0.0001") @DecimalMax("1.0") Double widthNorm,
    @NotNull @DecimalMin("0.0001") @DecimalMax("1.0") Double heightNorm,
    LocalDateTime deadline
) {}