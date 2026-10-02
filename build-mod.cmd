@echo off
set "JAVA_HOME=C:\Users\DecembeR\Documents\Codex\2026-09-29\new-chat\work\jdk25\jdk-25.0.4.1+1"
set "GRADLE_USER_HOME=C:\Users\DecembeR\Documents\Codex\2026-09-29\new-chat\work\gradle-cache"
set "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true"
cd /d "%~dp0"
call gradlew.bat build --no-configuration-cache
pause
