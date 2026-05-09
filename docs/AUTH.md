# Authentification — Documentation

## Vue d'ensemble

L'authentification repose sur deux tokens complémentaires :

- **Access token** (JWT, 15 min) — transmis dans le header `Authorization: Bearer <token>` à chaque appel API protégé.
- **Refresh token** (UUID opaque, 7 jours) — transmis uniquement via un cookie `HttpOnly`, jamais exposé au JavaScript.

---

## Flux nominal

### 1. Inscription / Connexion

```
POST /api/auth/register   { name, email, password }
POST /api/auth/login      { email, password }
```

**Réponse :**

```json
{ "accessToken": "<jwt 15 min>" }
```

```
Set-Cookie: refresh_token=<uuid>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

Le refresh token n'apparaît **jamais** dans le corps de la réponse.

---

### 2. Appel d'une route protégée

```
GET /api/patches
Authorization: Bearer <accessToken>
```

Le filtre `JwtAuthenticationFilter` vérifie :
- Signature HMAC-SHA256 valide.
- Token non expiré.
- Claim `type == "access"` présent (empêche l'usage d'un refresh token comme access token).

---

### 3. Renouvellement (rotation du refresh token)

Lorsque l'access token expire, le client appelle :

```
POST /api/auth/refresh
Cookie: refresh_token=<ancien uuid>
```

**Réponse :**

```json
{ "accessToken": "<nouveau jwt 15 min>" }
```

```
Set-Cookie: refresh_token=<nouvel uuid>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

L'ancien refresh token est **immédiatement révoqué** en base. Chaque token ne peut être utilisé qu'**une seule fois**.

---

### 4. Déconnexion

```
POST /api/auth/logout
Cookie: refresh_token=<uuid>
```

- Tous les refresh tokens actifs de l'utilisateur sont révoqués en base.
- Le cookie est effacé (`Max-Age=0`).
- L'access token en cours reste techniquement valide jusqu'à son expiration naturelle (15 min) — sa durée courte limite l'impact.

---

## Reuse Detection (détection de vol)

Chaque chaîne de rotation appartient à une **famille** (UUID partagé). Si un refresh token **déjà révoqué** est présenté :

1. Le serveur détecte la réutilisation.
2. **Toute la famille est révoquée** — toutes les sessions issues de cette connexion initiale sont invalidées.
3. La réponse est `401 Unauthorized`.

Ce mécanisme garantit que si un attaquant vole un refresh token et l'utilise, la victime est déconnectée dès sa prochaine tentative de rafraîchissement (et vice-versa), alertant ainsi implicitement d'une compromission.

---

## Stockage des refresh tokens en base

| Champ        | Type      | Description                                          |
|--------------|-----------|------------------------------------------------------|
| `tokenHash`  | `VARCHAR` | SHA-256 du token brut (unique) — le token brut n'est jamais stocké |
| `user`       | FK        | Propriétaire du token                                |
| `family`     | `VARCHAR` | UUID de la chaîne de rotation                        |
| `expiresAt`  | `TIMESTAMP` | Expiration absolue (7 jours par défaut)             |
| `revoked`    | `BOOLEAN` | `true` si le token a été consommé ou révoqué         |

Un job `@Scheduled` s'exécute chaque nuit à 3h pour purger les lignes expirées.

---

## Sécurité du cookie

| Attribut       | Valeur        | Justification                                              |
|----------------|---------------|------------------------------------------------------------|
| `HttpOnly`     | `true`        | Inaccessible au JavaScript — protège contre le vol par XSS |
| `Secure`       | `true`        | Transmis uniquement en HTTPS                               |
| `SameSite`     | `Strict`      | Le cookie n'est pas envoyé lors de requêtes cross-site — atténue le CSRF |
| `Path`         | `/api/auth`   | Scope minimal — le cookie n'est envoyé qu'aux endpoints d'auth |
| `Max-Age`      | `604800` (7j) | Expiration côté navigateur cohérente avec la base          |

> **Développement local (HTTP)** : retirer temporairement l'attribut `Secure` dans `AuthController.addRefreshCookie()`.

---

## Endpoints

| Méthode | Chemin              | Auth requise | Description                          |
|---------|---------------------|--------------|--------------------------------------|
| `POST`  | `/api/auth/register`| Non          | Crée un compte et retourne une session |
| `POST`  | `/api/auth/login`   | Non          | Authentifie et retourne une session  |
| `POST`  | `/api/auth/refresh` | Cookie       | Rotation du refresh token            |
| `POST`  | `/api/auth/logout`  | Cookie (opt.)| Révoque la session et efface le cookie |

---

## Configuration

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}           # Clé HMAC-SHA256 (min. 256 bits)
    expiration-ms: ${JWT_EXPIRATION_MS:900000}         # 15 minutes
  refresh-token:
    expiration-days: ${REFRESH_TOKEN_EXPIRATION_DAYS:7}
```

`JWT_SECRET` doit être une chaîne aléatoire d'au moins 32 caractères, injectée via variable d'environnement en production — ne jamais committer une valeur réelle.
