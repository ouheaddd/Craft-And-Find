@echo off
set APP_HOME=%~dp0
set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo Missing gradle\wrapper\gradle-wrapper.jar
  echo Copy it from the official NeoForge 1.21.1 MDK, then run gradlew runClient or gradlew build.
  exit /b 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
