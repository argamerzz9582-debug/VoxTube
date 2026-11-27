# Security Policy

## Supported versions

Security fixes land on the latest release line only. Older versions do not
receive backported patches.

| Version | Supported |
| ------- | --------- |
| 2.2.x   | ✅ |
| < 2.2.0 | ❌ |

Always update to the newest release before reporting a security issue.

## Reporting a vulnerability

**Do not report security vulnerabilities through public GitHub issues.**

Report privately through
[GitHub Security Advisories](https://github.com/argamerzz9582-debug/VoxTube/security/advisories/new).

Please include:

- Type of issue (for example: credential exposure, path traversal, injection).
- Full paths of the source files involved.
- The affected tag, branch, or commit.
- Any configuration required to reproduce the issue.
- Step-by-step reproduction instructions.
- Proof-of-concept or exploit code, if available.
- Impact, including how an attacker might exploit it.

You will receive an acknowledgement within 48 hours and a timeline for a fix.
Please do not disclose the issue publicly until a fix has shipped.

## Verifying release APKs

Releases are currently signed with the **public fallback key** described in
[`.github/signing/README.md`](.github/signing/README.md). Its private key is in
this repository, so the fingerprint below does not prove who built an APK.
Only install VoxTube from the
[GitHub Releases page](https://github.com/argamerzz9582-debug/VoxTube/releases),
and compare the file with the `checksums.txt` attached to the same release.

```
SHA-256: 37:9C:1A:5E:DC:DF:4C:DE:21:B7:67:7F:5B:D5:E1:54:CE:A1:F5:33:9C:1C:75:D2:CF:A3:E3:BF:15:3C:A4:43
```

```bash
apksigner verify --print-certs voxtube.apk
sha256sum voxtube.apk   # compare with checksums.txt
```
