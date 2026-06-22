package com.mangastudio.backend.dto;

import jakarta.validation.constraints.*;

public record CreateEditorAnnotationRequest(
    @NotNull Long editorId,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double xNorm,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double yNorm,
    @NotNull @DecimalMin("0.0001") @DecimalMax("1.0") Double widthNorm,
    @NotNull @DecimalMin("0.0001") @DecimalMax("1.0") Double heightNorm,
    @NotBlank(message = "Comment cannot be empty") String comment
) {}