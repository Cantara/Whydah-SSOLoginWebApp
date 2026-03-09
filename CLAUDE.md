# Whydah-SSOLoginWebApp

## Purpose
The web frontend for Whydah SSO services. Provides the user-facing login page, session management, and SSO flow coordination. This is the entry point users interact with when logging into any Whydah-protected application.

## Tech Stack
- Language: Java 21
- Framework: Jersey 1.x, Spring 6.x, Spring Security 6.x, Jetty 12.x
- Build: Maven
- Key dependencies: Whydah-Admin-SDK, Spring Security, Jetty, Dropwizard Metrics

## Architecture
Standalone web application with embedded Jetty server. Handles user authentication flows including username/password login, social login (Facebook, OAuth), and SSO session management. Communicates with SecurityTokenService for token generation and UserIdentityBackend for identity storage. Supports DEV mode for testing with pre-configured user tokens.

## Key Entry Points
- `/sso/login` - Login page
- `/sso/health` - Health check (port 9997)
- `start_service.sh` - Service startup script
- `update_service.sh` - Service update script

## Development
```bash
# Build
mvn clean install

# Run
java -jar target/SSOLoginWebApp-*.jar

# Verify
curl http://localhost:9997/sso/health
```

## Domain Context
User authentication frontend for the Whydah IAM ecosystem. The primary user-facing component that handles login flows, session cookies, and SSO ticket management across all Whydah-protected applications.
