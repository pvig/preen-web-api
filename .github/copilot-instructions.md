# preenWebApi — Instructions pour GitHub Copilot

## Stack
- Spring Boot 3.5.13 / Java 21
- JJWT 0.12.6, Spring Data JPA, Spring Security, SpringDoc OpenAPI, Lombok, BCrypt
- PostgreSQL (prod) / H2 (tests)
- Build : Gradle Kotlin DSL (`./gradlew`)

## Structure des packages
Organisation **par domaine fonctionnel** (package-by-feature), flat dans chaque domaine :
```
com.pvig.preenWebApi/
├── auth/      (AuthController, AuthService, JwtService, JwtAuthenticationFilter,
│               RefreshTokenEntity, RefreshTokenRepository, RefreshTokenService,
│               RefreshTokenCleanupJob, TokenPair, AuthResponseDto,
│               LoginRequestDto, RegisterRequestDto)
├── user/      (User, Role, UserRepository, UserController, UserProfileDto)
├── patch/     (PatchEntity, PatchRepository, PatchController, PatchService,
│               PublishedPatchDto, PublishPatchRequestDto)
├── comment/   (CommentEntity, CommentRepository, CommentController, CommentService,
│               CommentDto, CommentRequestDto)
└── config/    (ApplicationConfig, SecurityConfig, OpenApiConfig)
```

- Ne pas créer de sous-packages `controller/`, `service/`, `dto/` à l'intérieur d'un domaine.
- Tout nouveau domaine métier suit la même organisation flat.
- Ne pas recréer les anciens packages techniques racines (`entity/`, `repository/`, `service/`, `controller/`, `dto/`, `security/`).

## Authentification
- Access token JWT **15 min**, claim `type: "access"` obligatoire — vérifié dans `JwtAuthenticationFilter`.
- Refresh token UUID opaque, haché SHA-256 avant stockage, transmis **uniquement via cookie HttpOnly**.
- Rotation à chaque usage + reuse detection par famille d'UUID (voir `RefreshTokenService`).
- Méthode à utiliser : `jwtService.generateAccessToken(user)` (pas `generateToken`).
- Documentation complète : `docs/AUTH.md`.

## Conventions de code
- Utiliser **Lombok** (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@NoArgsConstructor`) sur les entités et services.
- Les DTOs sont des **records** Java.
- Les entités JPA n'implémentent pas d'interface sauf `User` qui implémente `UserDetails`.
- Les erreurs métier sont levées avec `ResponseStatusException`.
- Pas de `@Transactional` sur les controllers — uniquement sur les services.

## Tests
- Tests d'intégration avec `@SpringBootTest` + `@AutoConfigureMockMvc` sur H2.
- Fichier de test principal : `PatchControllerTest`.

## Documentation
- Fichiers markdown dans `docs/`.
- `docs/AUTH.md` : fonctionnement de l'authentification.

## Configuration (application.yaml)
- `app.jwt.secret` → variable d'env `JWT_SECRET` (jamais committer une valeur réelle)
- `app.jwt.expiration-ms` → 900000 (15 min)
- `app.refresh-token.expiration-days` → 7

## Commandes utiles
- `./gradlew compileJava` — compiler
- `./gradlew test` — lancer les tests
- `./gradlew bootRun` — démarrer l'application
