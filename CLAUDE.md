# WATTOM Guidelines
- **Stack:** Java 21+, Virtual Threads, JFR streaming, ByteBuddy.
- **Build:** `./gradlew clean build -Xlint:all -Werror`
- **Rules:** Zero frameworks. Catch all Throwables. Never block carrier threads. Run tests before committing.
