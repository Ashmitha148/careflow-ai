@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script for CareFlow AI
@REM ----------------------------------------------------------------------------

@IF "%__JAVA_HUB_DEBUG__%"=="1" echo on

@SETLOCAL enableextensions

@SET "MAVEN_BASEDIR=%~dp0"
@IF "%MAVEN_BASEDIR:~-1%"=="\" SET "MAVEN_BASEDIR=%MAVEN_BASEDIR:~0,-1%"

@IF EXIST "%USERPROFILE%\.m2\apache-maven-3.9.6\bin\mvn.cmd" (
  "%USERPROFILE%\.m2\apache-maven-3.9.6\bin\mvn.cmd" %*
) ELSE IF NOT "%MAVEN_HOME%"=="" (
  "%MAVEN_HOME%\bin\mvn.cmd" %*
) ELSE (
  mvn %*
)
