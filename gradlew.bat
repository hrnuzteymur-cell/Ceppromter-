@rem Cep Prompter Gradle wrapper
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
java -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
