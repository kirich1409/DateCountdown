# Security Policy

## Supported Versions

Only the latest released version of DateCountdown receives security fixes. Older versions are not supported.

## Reporting a Vulnerability

Please report security vulnerabilities through one of the following channels:

1. **GitHub private security advisory** — go to the repository page, navigate to **Security → Report a vulnerability**, and submit the report there. This keeps the details private until a fix is ready.
2. **Email** — send details to **kirill@androidbroadcast.dev**.

You can expect an initial response within **7 days**.

## Attack Surface Notes

The app declares no `INTERNET` permission — all event data is stored locally on-device and never transmitted over the network. `allowBackup=false` is set in the manifest, so data is not backed up to cloud storage. A reporting channel is provided regardless, as local-only apps can still have vulnerabilities (e.g. in file handling, IPC, or local data storage).
