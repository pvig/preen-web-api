package com.pvig.preenWebApi.patch;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublishPatchRequestDto(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 500) String description,
        List<String> tags,
        @NotNull JsonNode patchData
) {
}
