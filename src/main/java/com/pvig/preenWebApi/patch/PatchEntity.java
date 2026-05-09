package com.pvig.preenWebApi.patch;

import com.pvig.preenWebApi.comment.CommentEntity;
import com.pvig.preenWebApi.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patches")
@Getter
@Setter
@NoArgsConstructor
public class PatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "patch_tags", joinColumns = @JoinColumn(name = "patch_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String patchData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private User author;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "patch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();
}
