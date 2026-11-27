# Signing

`voxtube-public.p12` is a **public fallback key**. CI signs release and nightly
APKs with it only when the `RELEASE_KEYSTORE_BASE64` / `NIGHTLY_KEYSTORE_BASE64`
secrets are not set, so every build installs and updates over the previous one.

| | |
|---|---|
| Store type | PKCS12 |
| Alias | `voxtube` |
| Store and key password | `voxtube-public` |
| Certificate SHA-256 | `379c1a5edcdf4cde21b7677f5bd5e154cea1f5339c1c75d2cfa3e3bf153ca443` |

Its private key is in this public repository, so anyone can sign an APK with it.
Only install VoxTube from this repository's Releases page.

## Switching to your own key

1. Create a key: `keytool -genkeypair -keystore release.keystore -alias voxtube -keyalg RSA -keysize 4096 -validity 10000`
2. Add these repository secrets: `RELEASE_KEYSTORE_BASE64` (`base64 -w0 release.keystore`),
   `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Add the `NIGHTLY_*` set for nightlies.
3. Put the new certificate digest in `EXPECTED_SIGNER_SHA256` /
   `EXPECTED_NIGHTLY_SIGNER_SHA256` in `.github/workflows/build.yml`.

Android only installs an update signed with the same key, so after the switch
users have to uninstall the fallback-signed build once.
