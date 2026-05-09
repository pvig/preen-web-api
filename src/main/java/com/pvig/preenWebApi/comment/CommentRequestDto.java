package com.pvig.preenWebApi.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequestDto(
        @NotBlank @Size(max = 500) String content
) {
}
