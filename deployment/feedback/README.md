# TEMPER feedback service: one Linux VPS

This kit prepares a separate HTTPS endpoint for explicitly reviewed, rated conversation feedback. Ordinary analysis stays on-device. It neither provisions a paid server/domain nor enables collection in the Android app. The owner currently has no server or domain; the example leaves these values empty and cannot start a service.

Use one Linux VPS with a persistent local disk and an already installed Docker Engine/Compose v2.24+. This filesystem store must have exactly one writer; do not use replicas, an ephemeral container host, Docker Swarm or Kubernetes. The scripts expect the local default Docker daemon with standard UID mapping. Rootless/user-namespace remapping needs a separately verified ownership recipe and is refused here.

## Files and isolation

- `compose.yaml`: non-root feedback (UID10001) and Caddy (UID10101), read-only roots, dropped capabilities, memory/process limits. Only Caddy publishes host80/443. Feedback has no published port or outbound network; it is reachable only on an internal Docker network.
- `Caddyfile`: automatic HTTPS, only the health/submit/delete routes, 384000-byte request limit and timeouts. Neither access logs nor runtime request-error metadata are retained. Caddy's admin API is disabled. Docker retains only bounded operational application output; Spring request-detail/access logging is disabled. Never enable debug/HTTP tracing, packet capture, heap dumps or log request bodies/tokens.
- `Dockerfile` / `FeedbackHealth.java`: existing backend built with Java21, one-purpose feedback flags, local health check without additional curl packages. Build images use explicit version tags; refresh/review their immutable digests in the release environment for a fully pinned supply chain. Caddy runtime requires an owner-reviewed official image digest.
- `feedback-deploy.py`: Python3 standard-library bootstrap, preflight and optional start. It parses constrained configuration directly and never sources shell code or prints encryption keys. Initialization never overwrites an existing key. Portable checks do not start containers or contact DNS/HTTP.

The private host root is `/srv/temper-feedback` (root,0700). Records are UID10001,0700; the AES256 file is UID10001,0400 inside a root-only secrets directory. Caddy data/config are UID10101,0700. The AES value is never placed in environment variables. Compose file secrets retain their host ownership; setting `uid` or `mode` in Compose would not fix a file secret's permissions. See [Docker secrets](https://docs.docker.com/compose/how-tos/use-secrets/) and the [Compose specification](https://compose-spec.github.io/compose-spec/spec.html#secrets).

## Operator setup after obtaining a server/domain

1. Choose a VPS/data region, verify provider security and privacy terms, disable automated snapshots/backups of this data volume, install Docker/Compose using the provider or [official Docker instructions](https://docs.docker.com/engine/install/). This kit does not install system software. Allow SSH only from the owner's administration addresses, and public TCP80/443. Do not expose8080,8443 or the Docker daemon remotely. Check both the provider firewall and Docker-published ports.
2. Point one real hostname's A record at the VPS; only add AAAA if IPv6 routing/firewall actually work. DNS must resolve to this server. Caddy needs reachable80/443 and outbound certificate-authority access; [automatic HTTPS prerequisites](https://caddyserver.com/docs/automatic-https) apply. The internal Caddy ports8080/8443 map to standard public80/443, allowing its container to stay non-root.
3. Copy the reviewed source checkout to the server. Copy `feedback.env.example` to `/etc/temper-feedback.env`, chmod0600 and fill the actual hostname, certificate email and reviewed `caddy:2@sha256:...` image reference. Keep `FEEDBACK_ROOT=/srv/temper-feedback`. Use the current patched official Caddy release and inspect its digest on that host; no digest or domain is invented here.
4. From the repository root, prepare and verify the host:

   ```sh
   sudo python3 deployment/feedback/feedback-deploy.py --init
   sudo python3 deployment/feedback/feedback-deploy.py --preflight
   ```

   Initialization only creates private directories and a random32-byte Base64 AES key. It does not install, build, start or deploy services. Preserve that key for the lifetime of the existing records; replacing it makes them unreadable. Preflight validates permissions/local Docker/Compose but cannot prove DNS, public TLS or policy compliance.

5. When the real host is authorized/configured, start the reviewed kit:

   ```sh
   sudo python3 deployment/feedback/feedback-deploy.py --start
   sudo docker compose --env-file /etc/temper-feedback.env -f deployment/feedback/compose.yaml ps
   ```

   Start first validates Caddy configuration, then builds and starts exactly one feedback instance. A running container is not proof of successful public certificate issuance. Check `https://YOUR_ACTUAL_HOST/actuator/health` from another machine with standard TLS verification; it must return only health status. Unsupported routes must return404, unauthenticated submission must return401, and oversize payloads must return413. Test submission/idempotent retry/deletion/replay rejection only with a fictional reviewed session and a temporary deletion token; never print its token or submit real conversations for this check.

6. Publish the actual privacy policy/support contact and name the provider/region/retention/deletion practices. Configure edge abuse protection before broad public collection. Stock Caddy has no rate-limit directive in this kit: backend concurrency/record/contributor bounds prevent unbounded storage but do not stop attackers minting deletion tokens or exhausting capacity. An upstream WAF/rate limiter must be configured without body/Authorization logging, and origin access restricted so it cannot be bypassed. Do not describe the current anonymous endpoint as authenticated membership or unlimited public-scale storage.
7. Build the Android release with `-PtemperLearningUrl=https://YOUR_ACTUAL_HOST`, complete end-to-end fictional phone send/delete checks and then distribute the signed app. The default APK contains no endpoint and still cannot upload feedback. There is no collection activation command in this deployment kit.

## Retention and operation

The backend encrypts submissions and expires records/revocation tombstones after90 days, with an hourly scheduler plus access/startup purge. Monitor container health, available disk, record/contributor capacity, deletion failures and actual expiry using generated fixtures. A failed scheduler must be corrected before continued collection; a health response alone does not prove data retention. Do not tail record files, decrypt production records into logs or enable an HTTP export route.

Back up neither the encrypted record volume nor raw export/reviewer/training copies independently until deletion propagates through every copy and its retention is enforced. This initial configuration deliberately has no scheduled backup. Provider snapshots must also be disabled. The tradeoff is that host/key failure can lose contributions; no disaster recovery is claimed. Caddy certificate state contains private keys and needs the same restricted operator access.

Use the documented offline export/review workflow only on an encrypted private operator volume. Immediately invalidate/delete prior plaintext exports and related working copies when a contributor is deleted or expires, before any training run. The current freshness gate helps bound stale exports but does not replace this operational deletion process. Users provide ratings only; independent reviewers supply training labels. No model update is automatically deployed.

Restarting/rebuilding keeps the bind-mounted records and existing AES key. Stop with `docker compose ... down` (using the same env/file arguments); do not remove `/srv/temper-feedback`, replace the key or rotate ownership recursively. To change the Caddyfile, restart its container because the admin reload API is disabled. Apply patched Java/Caddy/base images and repeat fictional HTTPS checks before a public release.

## Verification available here

```sh
python3 deployment/feedback/feedback-deploy.py --check
python3 deployment/feedback/test-deployment.py
```

These portable checks cover missing/example/unsafe configuration, no-shell parsing, exclusive AES key creation and deployment-containment invariants using temporary synthetic files. Official Caddy2.11.6 independently adapted and validated this Caddyfile on the development machine with a fictional hostname, without starting a server. The official standalone Docker Compose5.5.1 parser also passed `config --quiet` with a fictional configuration, without a daemon or containers. These parser checks do not verify container builds, Linux UID mounts, DNS, public TLS or real service operation. Full Compose build and real VPS validation remain outstanding because Docker/server/domain are unavailable.
