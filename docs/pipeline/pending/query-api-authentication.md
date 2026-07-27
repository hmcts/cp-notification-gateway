# Query-API authentication — analysis and options (NG-S14 / FR-024)

**Status:** ⏳ **Decision pending** — analysis complete, pattern not yet chosen. Owned by
**NG-S14** (sliced out of NG-S04, which now covers only the read API itself and ships it
deny-all-by-default). Held in `docs/pipeline/pending/`; to be resolved once the exposure/consumer
questions in §7 are answered, and an ADR raised before implementation begins.

**Scope:** How to secure the read (query) REST API delivered by NG-S04
(`GET /notifications/{id}`, `GET /notifications?…`) so that AC-039 holds — an unauthenticated
request is rejected (401/403) and a valid caller is served — without ever exposing OFFICIAL-SENSITIVE
PII (`send_to_address`) unauthenticated (NFR-004/005/006).

> This corrects `architecture-design.md`, which states *"IDAM JWT via `cp-auth-rules-filter`
> (per `cpp-mbd-idam-integration`)"*. Two facts there are wrong: (a) `cpp-mbd-idam-integration`
> is a permit-all POC event consumer with no REST surface — not an auth exemplar; (b)
> `cp-auth-rules-filter` does **not** validate an IDAM JWT, it trusts a gateway-injected header.
> The real exemplars are `cp-court-list-publishing-service` / `service-cp-crime-hearing-results-validator`
> (Pattern 1) and `cpp-mbd-notification-svc-pilot` (Pattern 2).

## 1. Three identity planes (do not conflate)

| Plane | Role | Who owns it |
|-------|------|-------------|
| **IDAM** | Authenticates *people* (login, OIDC). Emits account/org lifecycle events. | HMCTS IDAM |
| **Users & Groups (U&G)** | The platform's *authorisation* store — groups, roles, permissions that legacy access-control rules query. | `cpp-context-users-groups` |
| **Entra ID (Azure AD)** | The *Azure* identity plane. A **different issuer from IDAM** — mints OAuth2 tokens and holds App Roles. | Azure tenant (Platform Engineering) |

`cpp-mbd-idam-integration` only bridges IDAM → U&G; it does not decide who may call anything.

## 2. Pattern 1 — `cp-auth-rules-filter` (header-trust + U&G + Drools)

Used by `cp-court-list-publishing-service` and `service-cp-crime-hearing-results-validator`.
**It validates no token cryptographically.** Trust is by network/gateway convention:

1. The user logs in against **IDAM** at the edge (`idam-am-gateway`); a session is established.
2. The fronting gateway verifies the session and injects a **`CJSCPPUID`** header (user UUID)
   into the backend request — stripping any client-supplied `CJSCPPUID` first.
3. The service **trusts that header** — no signing, bearer token, or mTLS protects it.
4. `cp-auth-rules-filter` reads `CJSCPPUID` + a `CPP-ACTION` header, calls the U&G query-API
   once per request for that user's permissions, then evaluates the `.drl` Drools rules
   (`hasPermission` / `isMemberOfAnyOfTheSuppliedGroups`).
5. System/background traffic uses a pre-provisioned **system-user UUID**, authorised by the same
   rules.

The HS256 `JWTFilter` visible in `cp-court-list-publishing-service`'s `application-jwt.yml` is
`jwt.filter.enabled: false` by default — **not** the operative production mechanism.

Wiring (from the exemplars):

```gradle
implementation "uk.gov.hmcts.cp:cp-auth-rules-filter:1.0.7"
implementation "uk.gov.hmcts.cp:cp-audit-filter-springboot:1.0.5"
```

```yaml
authz:
  http:
    enabled: ${AUTHZ_ENABLED:true}
    identity-url-template: ${CP_BASE_URL}/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions
    user-id-header: "CJSCPPUID"
    action-header: "CPP-ACTION"
    drools-classpath-pattern: "classpath:/acl/**/*.drl"
    deny-when-no-rules: true
    exclude-path-prefixes: ["/actuator", "/error"]
```

**Security rests entirely on the network boundary** — the service must be unreachable except via
the trusted gateway.

## 3. Pattern 2 — Spring Security OAuth2 Resource Server against Entra ID (real JWT)

Used by `cpp-mbd-notification-svc-pilot` — the sanctioned "Modern by Default reference
implementation… the architectural blueprint every new CPP service should copy". This does genuine
cryptographic validation:

1. The caller obtains a **JWT from Entra ID** — a machine via **client-credentials**
   (service principal / workload-identity federation), a human via **authorization-code / OIDC**.
2. Caller presents `Authorization: Bearer <entra-jwt>`.
3. `SecurityConfig` validates the token signature against Entra's **JWKS**
   (`spring.security.oauth2.resourceserver.jwt.issuer-uri`).
4. A custom `JwtAuthenticationConverter` maps the token's **`roles` claim → `ROLE_*`** authorities.
5. Controllers gate with `@PreAuthorize("hasAnyRole(...)")`; `anyRequest().denyAll()` denies by
   default. Only actuator health/info/prometheus and Swagger are public.
6. A `local`-only profile swaps in a permissive decoder for dev — never for a deployed environment.

Wiring sketch:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/${TENANT_ID}/v2.0
          audiences: api://${NOTIFICATION_GATEWAY_APP_ID}
```

```java
http
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
      .requestMatchers("/notifications/**").hasAnyRole("NOTIFICATION_READ")
      .anyRequest().denyAll())
  .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())));
```

## 4. "Azure RBAC" — two different senses

- **Resource-plane RBAC (already in this service).** Workload Identity + `DefaultAzureCredential`
  + role assignments (`Azure Service Bus Data Receiver/Sender`, `Storage Blob Data Reader`).
  This authorises the service's **own outbound access** to ASB and Blob. It is outbound only and
  **cannot** secure the inbound read API. "We use managed identity for ASB" ≠ "the API is secured".
- **Entra app auth + App Roles (= Pattern 2).** Callers get Entra tokens; the API validates them
  and checks App Roles/scopes. Because the managed identities already live in the same Entra tenant,
  this fits **machine** callers cleanly (workload-identity federation → client-credentials → app
  role). For **human** support/ops users it requires onboarding them into Entra with app-role
  assignments — their CPP roles otherwise live only in IDAM + U&G.
- **APIM edge-enforced (topology variant).** Azure APIM can run a `validate-jwt` policy at the edge
  (Entra or IDAM) and the service trusts the network. Already relevant since O365 is routed via APIM.

## 5. Decision matrix

| | Pattern 1: `cp-auth-rules-filter` | Pattern 2: Entra OAuth2 (Azure RBAC) |
|---|---|---|
| Token validated in-service? | **No** — trusts `CJSCPPUID` from gateway | **Yes** — Entra JWT via JWKS |
| Identity source | IDAM login → U&G groups | Entra ID + App Roles |
| Best fit caller | CPP **humans** + system-users behind the gateway | **Machine** callers / Azure workloads |
| Satisfies AC-039? | Yes (`deny-when-no-rules`) | Yes (`denyAll()` default) |
| Security depends on | **Network boundary** (gateway trust) | **Cryptography** (token signature) |
| Estate precedent | 2 live services | 1 reference pilot (send step stubbed) |
| Boot 4 support | verify `cp-auth-rules-filter` 1.0.7 on Boot 4 | native to Spring Boot |

## 6. Recommendation

**Pattern 2 (Entra OAuth2 resource server)** is the stronger fit for NG-S04: the persona is
*"support/operations engineer **or an authenticated machine consumer**"*, the data is
OFFICIAL-SENSITIVE PII (NFR-005), and Pattern 2 gives real cryptographic validation independent of
network trust while reusing the Entra identity plane already in the service. It also copies the
sanctioned MbD blueprint.

Choose **Pattern 1** only if this endpoint is guaranteed to be reachable *only* through the trusted
IDAM gateway and its consumers are CPP humans/system-users — in which case record the
network-boundary-trust assumption explicitly in the ADR.

There is **no platform-sanctioned winner** — this is a genuine open decision and warrants an ADR
before implementation.

## 7. Open questions (gate the ADR)

1. **Exposure** — is the read API behind the fronting IDAM gateway (like court-list), or a
   standalone ingress reachable by machine consumers? This decides Pattern 1 vs 2 more than anything.
2. **Day-one consumer** — the mi-reportdata pipeline has no consumer yet; is the first real caller a
   human in a support tool, or a machine/monitoring job?
3. **Boot 4 compatibility** (Pattern 1 only) — confirm `cp-auth-rules-filter:1.0.7` /
   `cp-audit-filter-springboot:1.0.5` work on Spring Boot 4.1 before wiring.
4. **Invalid-token status** (Pattern 1's `JWTFilter`) — returns 400 for an invalid token; AC-039
   wants 401/403. N/A for Pattern 2 (resource server returns 401).

---

## 8. How to manage Entra App Roles (Pattern 2)

App Roles are Entra's RBAC for an API. They are defined on the **App Registration that represents
this API** (the token audience), and assigned to the identities allowed to call it. In CPP this is a
**Platform Engineering-owned** action and should be codified in Terraform (matching the
managed-identity / GitOps model in NG-S08/NG-S09), not clicked once by hand.

### 8.1 Define a role (on the API's app registration)

Portal: **Entra ID → App registrations → [notification-gateway API] → App roles → Create app role**.

| Field | Example | Notes |
|-------|---------|-------|
| Display name | `Notification Reader` | Human label |
| Value | `Notification.Read` | The string that lands in the token's `roles` claim |
| Allowed member types | see below | Governs which token type carries the role |
| Description | `Read notification status and search` | |

**Allowed member types** is the key choice:
- **Applications** → assignable to other apps/service principals → appears in an **app-only** token
  (client-credentials, machine callers).
- **Users/Groups** → assignable to users/groups → appears in a **delegated (user)** token.
- **Both** → either.

Terraform equivalent (illustrative):

```hcl
resource "azuread_application_app_role" "notification_read" {
  application_id       = azuread_application.notification_gateway_api.id
  role_id              = "…uuid…"
  value                = "Notification.Read"
  display_name         = "Notification Reader"
  description          = "Read notification status and search"
  allowed_member_types = ["Application", "User"]
}
```

### 8.2 Assign a role

- **Machine / service principal:** on the **client** app registration →
  **API permissions → Add a permission → My APIs → [notification-gateway API] → Application
  permissions → Notification.Read → Add**, then **Grant admin consent**. This creates an
  `appRoleAssignment` on the API's service principal. Terraform: `azuread_app_role_assignment`.
- **User / group:** **Entra ID → Enterprise applications → [notification-gateway API service
  principal] → Users and groups → Add user/group → pick the App Role**. Assigning a role to a
  *group* requires Entra ID P1/P2. Set the service principal's `appRoleAssignmentRequired = true`
  so unassigned identities are refused a token.
- **CLI / Graph:** `az ad app update --app-roles @roles.json`; assignments via Microsoft Graph
  `appRoleAssignedTo`.

The Spring `JwtAuthenticationConverter` then maps each `roles` value → `ROLE_<value>` so
`@PreAuthorize("hasRole('Notification.Read')")` (or a mapped alias) gates the endpoint.

## 9. How a human obtains a JWT — and is there an Azure UI?

There is **no single "generate a JWT for my API" button** in the Azure portal. Humans get a token
through one of these, in rough order of convenience for testing:

1. **Azure CLI (quickest).** After `az login`:
   ```bash
   az account get-access-token \
     --scope "api://<notification-gateway-app-id>/.default" \
     --query accessToken -o tsv
   ```
   Paste the result as `Authorization: Bearer <token>`. Uses the signed-in user's identity; the
   user must hold an assigned App Role for `roles` to appear.

2. **Postman OAuth 2.0 helper (has a UI).** In a request's **Authorization → OAuth 2.0 → Get New
   Access Token**, choose **Authorization Code (with PKCE)** and fill Entra endpoints:
   - Auth URL: `https://login.microsoftonline.com/<tenant>/oauth2/v2.0/authorize`
   - Token URL: `https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token`
   - Scope: `api://<notification-gateway-app-id>/.default`
   Postman opens an interactive Microsoft login window, then stores the bearer token for reuse.

3. **Browser `/authorize` flow → `jwt.ms` (Microsoft's decode UI).** Construct an authorize URL
   with `redirect_uri=https://jwt.ms` (a Microsoft-hosted token-inspection page); after interactive
   login Entra redirects there and shows the decoded token and its claims. Good for "log in as a
   human and see the token/claims" — use auth-code + PKCE (implicit is deprecated and often
   disabled).

4. **Swagger "Authorize" button (in-app UI).** springdoc can be configured with an OAuth2
   (authorization-code) security scheme so the service's own Swagger page has an **Authorize**
   button that runs the Entra login and injects the bearer into try-it-out calls. This is the most
   consumer-friendly human UI and is worth wiring given the API-first OpenAPI story.

5. **Local dev — no real token.** Mirror the pilot's `@Profile("local") LocalDevConfig`: a
   permissive decoder that accepts any token and grants the roles, plus emulator messaging. Never
   enable this profile in a deployed environment.

Closest thing to an "Azure-provided UI": **Postman's OAuth flow**, **`jwt.ms`** (view/decode), and
the **Swagger Authorize button** if the API wires OAuth2. The portal itself administers *roles and
assignments*, not token minting; **Graph Explorer** issues tokens for Microsoft Graph only, not a
custom API.

## References (evidence)

- `cp-court-list-publishing-service` — Pattern 1: `build.gradle` (`cp-auth-rules-filter`),
  `src/main/resources/acl/court-list-publishing-rules.drl`, `src/main/resources/application-jwt.yml`,
  `src/main/java/uk/gov/hmcts/cp/acl/{SecurityGroupConstants,PermissionConstants}.java`.
- `service-cp-crime-hearing-results-validator` — Pattern 1: `gradle/libs.versions.toml`,
  `src/main/resources/application.yaml` (`authz.http`), `docs/JWTFilter.md`,
  `src/main/resources/acl/validation-rules.drl`.
- `cpp-mbd-notification-svc-pilot` — Pattern 2: `SecurityConfig` (OAuth2 resource server, Entra
  `issuer-uri`, `JwtAuthenticationConverter`, `@PreAuthorize`, `denyAll`), `LocalDevConfig`.
- CPP architecture KB (`cp-meta-arch`): `qa/platform.md` (two-patterns / CJSCPPUID trust model /
  IDAM-vs-U&G split), `components/cpp-mbd-notification-svc-pilot/README.md`.
</content>
</invoke>
