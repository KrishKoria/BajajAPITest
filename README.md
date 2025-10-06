# Bajaj API Test – Webhook Generation & SQL Submission

This Spring Boot project automates the 4‑step evaluation workflow:

1. Generate a webhook + JWT (startup POST to the hiring API)
2. (Manually / externally) receive & inspect webhook events (not implemented here because the remote service owns the callback behavior)
3. Derive the final SQL query (you provide it via configuration)
4. Automatically submit that SQL query back to the hiring API using the stored JWT + generated webhook endpoint

## Key Features
- Automatic webhook generation at startup (step 1)
- Captures and stores:
  - Webhook URL
  - JWT token
  - Raw response map (for future introspection / debugging)
- Automatic final query submission (step 4) using the stored JWT (supports both raw token and fallback `Bearer ` retry)
- Configurable delays, timeouts, and easy toggle to enable/disable either behavior
- Clean separation of concerns:
  - `webhook` package: acquisition of webhook + token
  - `solution` package: final SQL submission

## Runtime Flow
```
Application start
  -> WebhookStartupRunner
      -> WebhookGenerationService.generateAndStore()
          -> POST /hiring/generateWebhook/JAVA
          -> Store (webhookUrl, jwtToken, rawResponse)
  -> SolutionSubmissionRunner
      -> waits (optional) & then SolutionSubmissionService.submitIfConfigured()
          -> Reads stored jwtToken + webhookUrl
          -> POST final SQL to /hiring/testWebhook/JAVA
```

## Configuration
All configurable properties live in `application.properties` (or can be overridden via environment variables / command-line args):

Webhook generation (step 1):
```
webhook.generate.enabled=true
# webhook.generate.url=... (default already set in code)
# webhook.generate.name=John Doe
# webhook.generate.reg-no=REG12347
# webhook.generate.email=john@example.com
```

Solution submission (step 4):
```
solution.submit.enabled=true
# solution.submit.url=... (auto-fallback to stored webhook URL if available)
solution.submit.final-query=SELECT 1; -- replace with the REAL final query
# solution.submit.delay-ms=0
# solution.submit.wait-for-token-timeout-ms=10000
# solution.submit.wait-for-token-poll-ms=300
```

Override at launch (examples):
```
java -jar bajajapitest.jar ^
  --webhook.generate.name="Jane Doe" ^
  --solution.submit.final-query="SELECT * FROM your_table;" ^
  --solution.submit.delay-ms=2000
```

### Where to Put the Real Final SQL
Edit `application.properties`:
```
solution.submit.final-query=YOUR_FINAL_SQL_SINGLE_LINE_HERE
```
Keep it on one line (the service trims it). If you need newlines, you can escape them (`\n`) but generally a single-line canonical SQL is best.

## Building a Runnable JAR (Windows CMD)
Generate the Spring Boot fat jar directly into the project root.

Using the default Gradle Spring Boot task:
```cmd
gradlew.bat clean bootJar
copy build\libs\*-SNAPSHOT.jar bajajapitest.jar
```
Then run:
```cmd
java -jar bajajapitest.jar
```

(Optional) If you adjust `bootJar` in `build.gradle` to output to the root automatically:
```groovy
bootJar {
    archiveFileName = 'bajajapitest.jar'
    destinationDirectory = layout.projectDirectory.asFile
}
```
Then build + run:
```cmd
gradlew.bat clean bootJar
java -jar bajajapitest.jar
```

## Disabling Auto Submission
If you only want to generate the webhook & token (and NOT submit SQL yet):
```cmd
java -jar bajajapitest.jar --solution.submit.enabled=false
```
You can then observe logs for the token and prepare your query.

## Logs & Diagnostics
Typical successful startup log snippet:
```
Webhook URL: https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA
JWT token: <token>
Using dynamically generated webhook URL (stored): ...
Preparing final query submission (length=123 chars) to ...
```
401 handling example:
- First attempt uses raw token header (`Authorization: <token>`)
- If 401 and token lacked `Bearer `, a second attempt is made with `Authorization: Bearer <token>`

## Common Issues
| Symptom | Cause | Fix |
|---------|-------|-----|
| 401 Unauthorized | Token formatting mismatch | Let the retry logic handle or verify token not expired |
| Empty response on submission | API returns no body | Check logs; confirm query correctness |
| No webhook URL/token logged | Network/endpoint issue | Verify connectivity & that generation endpoint is reachable |
| Final SQL not submitted | `solution.submit.enabled=false` or blank `final-query` | Set property & restart |

## Extending
Ideas:
- Persist webhook + token to a file for reuse between runs
- Add an endpoint (e.g. `/introspect`) to expose stored webhook context
- Add integration tests using `WebClient` mock (e.g., MockWebServer)
- Include a validation step for the final SQL prior to submission

## Project Structure
```
webhook/
  WebhookGenerationService.java    # Fetch & store webhook + token
  WebhookContext.java              # In-memory storage
  WebhookStartupRunner.java        # Triggers generation at startup
solution/
  SolutionSubmissionService.java   # Submits final SQL
  SolutionSubmissionRunner.java    # Invokes submission after generation
  SolutionSubmissionProperties.java
```

## Tech Stack
- Java 23 (Toolchain)
- Spring Boot 4.0.0-M3 (WebFlux client only)
- Reactor (Mono / reactive HTTP)

## Reproducible Builds & Lint
Compiler flags enabled:
```
-Xlint:unchecked -Xlint:deprecation
```
Adjust in `build.gradle` if needed.

## Quick Start (All Defaults)
```cmd
gradlew.bat clean bootJar
java -jar bajajapitest.jar
```
Edit `application.properties` to insert your real SQL before running (or pass `--solution.submit.final-query=...`).

## Security Notes
- Token is only held in memory (not persisted)
- Logs currently print the raw JWT (as per task requirement). Remove or mask for production scenarios.

## License / Usage
Internal assessment utility. Adapt freely for the hiring task context.

---
If you need help crafting the final SQL or would like automated tests added, open an issue or extend the code—happy building!

