package com.pvig.preenWebApi.patch;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patches")
@RequiredArgsConstructor
public class PatchController {

    private final PatchService patchService;

    @GetMapping
    public ResponseEntity<Page<PublishedPatchDto>> getPatches(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "tags", required = false) String tags) {
        return ResponseEntity.ok(patchService.getPatches(page, size, search, tags));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublishedPatchDto> getPatchById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(patchService.getPatchById(id));
    }

    @PostMapping
    public ResponseEntity<PublishedPatchDto> createPatch(@Valid @RequestBody PublishPatchRequestDto request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(patchService.createPatch(request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatch(@PathVariable("id") UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        patchService.deletePatch(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
