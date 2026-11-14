# Forgot Password

The login page now links to `/forgot-password`. Recovery uses the existing users table,
BCrypt encoder, authentication and page styles. Reset tokens contain 32 random bytes;
only their SHA-256 hashes are stored. They expire after 15 minutes. Requesting another
link replaces the previous token. Consuming a token and updating the password happen
in one transaction, serialized using database locks. Successful resets invalidate
this application's active in-memory sessions for that user after the transaction commits.

Configuration (environment variables, or the existing local `.env` loader):

| Variable | Value |
| --- | --- |
| `SMTP_HOST` | SMTP server hostname |
| `SMTP_PORT` | SMTP port; default `587` |
| `SMTP_USERNAME` | SMTP username |
| `SMTP_PASSWORD` | SMTP password or provider app password |
| `SMTP_FROM` | Verified sender address |
| `SMTP_AUTH` | Default `true`; use `false` for a local mail catcher |
| `SMTP_STARTTLS` | Default `true`; use `false` for a local mail catcher |
| `APP_BASE_URL` | Public application URL, e.g. `https://tasks.example.com`; default `http://localhost:8080` |

No credentials are committed. Real SMTP delivery requires these settings. After submission the browser redirects to `/reset-password?sent=true`, which prompts
the user to open the emailed link. No account-existence confirmation popup is shown.
Delivery and missing-configuration errors are displayed instead of pretending to send.
Only the emailed token opens the password form. Use HTTPS for the public URL.

The existing `spring.jpa.hibernate.ddl-auto=update` setting adds only the new
`password_reset_tokens` entity table, with a unique foreign key to `users.user_id`,
a 64-character token hash primary key, and expiry timestamp. No destructive migration
is introduced. The feature changes an existing user's password only on successful reset.
The startup schema update and database-backed tests have been exercised against the
configured MySQL database; test-created user and token records roll back.

Recovery forms include session CSRF checks, no-store and no-referrer headers.
Limits per 15 minutes are 10 recovery requests per client IP, 3 per email address,
and 30 reset submissions per IP. Excess recovery requests display a retry message.
Limits and session tracking are local to this single application instance and restart
with it; clustered deployments need shared rate-limit and session storage. IP limits
use the servlet remote address, without trusting arbitrary forwarded headers.

Manual checks:

1. Set SMTP settings, start with `./mvnw.cmd spring-boot:run`, and open `/login`.
2. Click **Forgot Password?**, enter a registered email, and open the received link.
3. Enter mismatched passwords and verify the error. Then submit matching passwords
   of at least six characters. BCrypt supports at most 72 UTF-8 bytes.
4. Confirm the success message, sign in with the new password, and verify the old
   password fails. Any other signed-in browser for that user should require login again.
5. Reopen the used link; it must be rejected. Also try a malformed token, an expired
   link after 15 minutes, and the previous link after requesting a replacement.
6. Request recovery for an unregistered email; the same inbox-instructions page must appear.
7. Check existing registration, login, and task creation/edit/completion/deletion.

Automated checks: `./mvnw.cmd verify` with `DB_*` variables exported. The existing
application-context test and new integration tests require MySQL. The main application
loads `.env`, but Maven tests need the variables exported into their process.
Focused tests without MySQL:
`./mvnw.cmd "-Dtest=PasswordResetServiceTests,PasswordResetControllerTests" test`.
Email sending is mocked in tests; actual provider delivery needs the manual check.

New files (under `src/main` unless specified):

- `java/com/smarttask/config/PasswordResetSessions.java`
- `java/com/smarttask/controller/PasswordResetController.java`
- `java/com/smarttask/model/PasswordResetToken.java`
- `java/com/smarttask/repository/PasswordResetTokenRepository.java`
- `java/com/smarttask/service/PasswordResetRateLimiter.java`
- `java/com/smarttask/service/PasswordResetService.java`
- `resources/templates/forgot-password.html`
- `resources/templates/reset-password.html`
- `src/test/java/com/smarttask/controller/PasswordResetControllerTests.java`
- `src/test/java/com/smarttask/service/PasswordResetServiceTests.java`
- `src/test/java/com/smarttask/service/PasswordResetIntegrationTests.java`
- `FORGOT_PASSWORD.md` (this document)

Modified files:

- `pom.xml`: Spring Boot mail dependency.
- `.env.example`: SMTP and public URL placeholders.
- `src/main/resources/application.properties`: SMTP and recovery settings.
- `src/main/resources/templates/login.html`: recovery link.
- `src/main/java/com/smarttask/service/AuthService.java`: exposes the existing encoder for reset use.
- `src/main/java/com/smarttask/repository/UserRepository.java`: user-row locking query.

Gmail setup: SMTP host is smtp.gmail.com, port 587, STARTTLS and authentication
enabled. Set SMTP_USERNAME and SMTP_FROM to your sending Gmail address and
SMTP_PASSWORD to a Google App Password. Google requires 2-Step Verification for
App Passwords: https://support.google.com/accounts/answer/185833 .
The local .env now includes Gmail settings with blank sender credentials; fill them
in locally, then restart the application. Never share or commit your App Password.

Gmail setup, step by step:

1. Open https://myaccount.google.com/ and select the same Google account used by
   SMTP_USERNAME in .env. Check the profile icon to confirm the account.
2. Under Security, enable 2-Step Verification and finish the verification process.
3. Open https://myaccount.google.com/apppasswords while signed into that account.
4. Enter SmartTask as the app name and create an App Password.
5. Copy only the generated 16-character password. Do not copy the app name,
   quotation marks, explanatory text, or an ordinary Gmail password.
6. In .env, select the whole value after SMTP_PASSWORD= and replace it with the
   generated password, without spaces. Keep SMTP_USERNAME and SMTP_FROM set to
   the sending Gmail address. Keep SMTP_HOST=smtp.gmail.com and SMTP_PORT=587.
7. Save .env. Stop the running SmartTask process with Ctrl+C in its terminal.
   Restart from the project directory with .\mvnw.cmd spring-boot:run.
8. Open http://localhost:8080/forgot-password and submit an email registered in
   SmartTask. Check its inbox and spam folder, then use the emailed reset link.

SMTP 535 means Gmail rejected the credentials. Check that the App Password belongs
 to the account in SMTP_USERNAME, and generate a fresh one if necessary. The app
cannot create Google App Passwords for you. If App Passwords is unavailable, see
https://support.google.com/accounts/answer/185833 for account restrictions.
