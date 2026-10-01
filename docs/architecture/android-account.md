# Device account
This demo supports one local account per installation. Create an account under Device account with a 3–40 character username (letters, digits, underscore, dot or dash) and a 10–128 character password. Sign out preserves the account, clears the session and pauses TEMPER; sign in restores the local session. It does not authorize analysis-server requests or create a remote account.

Account and session state are encrypted with an Android Keystore AES-256-GCM key; passwords are verified with PBKDF2-HMAC-SHA256, 210,000 iterations and a random 16-byte salt. Password input buffers are cleared after processing. Backups and device transfer are excluded. No password or account plaintext is written to normal logs. There is no recovery flow; uninstalling removes local app data. Key invalidation or tampered ciphertext makes the account unavailable rather than bypassing authentication.

FoundationSmokeInstrumentation exercises crypto/session operations in a separate disposable preference/key namespace on the actual phone. This is local demo identity support, not a reviewed production authentication service.
