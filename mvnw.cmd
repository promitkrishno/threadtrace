@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@REM Begin all assignments locally
@echo off
@REM set title of command window
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@REM set %HOME% to equivalent of $HOME
if "%HOME%"=="" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Execute a user defined script before this one
if not "%MAVEN_SKIP_RC%"=="" goto skipRcPre
@REM check for pre script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat" %*
if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd" %*
:skipRcPre

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%"=="" goto OkJHome

echo. >&2
echo ERROR: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo. >&2
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo. >&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
echo. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo. >&2
goto error

@REM ==== END VALIDATION ====

:init

@REM Find the project basedir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\.mvn goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties" goto endReadWrapperProperties

@REM Read the wrapperUrl property from maven-wrapper.properties
for /F "usebackq tokens=1,2 delims==" %%A in ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") do (
    if "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
    if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
    if "%%A"=="wrapperSha256Sum" set WRAPPER_SHA256SUM=%%B
)

:endReadWrapperProperties

SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

@REM Download maven-wrapper.jar if it doesn't exist
IF EXIST %WRAPPER_JAR% goto runWrapper

@REM If the wrapper URL is not set, we cannot download the wrapper
if "%WRAPPER_URL%"=="" goto runWithoutWrapper

if "%MVNW_VERBOSE%"=="true" (
    echo Downloading from: %WRAPPER_URL%
)

@REM Download using PowerShell
powershell -Command ^
    "$ErrorActionPreference = 'Stop';" ^
    "if (-not (Test-Path -Path '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper' -PathType Container)) {" ^
    "    New-Item -Path '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper' -ItemType Directory | Out-Null;" ^
    "}" ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
    "$webclient = New-Object System.Net.WebClient;" ^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {" ^
    "    $webclient.Credentials = New-Object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');" ^
    "}" ^
    "$webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"
if "%MVNW_VERBOSE%"=="true" (
    echo Finished downloading %WRAPPER_JAR%
)

@REM Validate the SHA-256 checksum if specified
if "%WRAPPER_SHA256SUM%"=="" goto runWrapper
powershell -Command ^
    "$hash = (Get-FileHash -Path '%WRAPPER_JAR%' -Algorithm SHA256).Hash.ToLower();" ^
    "if ($hash -ne '%WRAPPER_SHA256SUM%') {" ^
    "    Write-Error 'ERROR: Failed to validate Maven wrapper SHA-256. Your Maven wrapper might be compromised.';" ^
    "    Write-Error 'Investigate or delete %WRAPPER_JAR% to attempt a clean download.';" ^
    "    exit 1;" ^
    "}"
if ERRORLEVEL 1 goto error

:runWrapper
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

"%JAVA_HOME%\bin\java.exe" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %MAVEN_ARGS% ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
if ERRORLEVEL 1 goto error
goto end

:runWithoutWrapper
@REM Fallback: download Maven distribution directly and run
set MAVEN_HOME=%HOME%\.m2\wrapper\dists

@REM Try to run Maven from the local installation
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    call "%MAVEN_HOME%\bin\mvn.cmd" ^
        %MAVEN_OPTS% ^
        %MAVEN_DEBUG_OPTS% ^
        "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
        %MAVEN_ARGS% ^
        %MAVEN_CONFIG% %*
    if ERRORLEVEL 1 goto error
    goto end
)

@REM Download Maven distribution using PowerShell
if "%DISTRIBUTION_URL%"=="" (
    echo ERROR: Could not find maven-wrapper.jar and no distributionUrl configured >&2
    goto error
)

echo Downloading Maven distribution from: %DISTRIBUTION_URL%
set MAVEN_ZIP=%TEMP%\apache-maven-dist.zip

powershell -Command ^
    "$ErrorActionPreference = 'Stop';" ^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
    "$webclient = New-Object System.Net.WebClient;" ^
    "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {" ^
    "    $webclient.Credentials = New-Object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');" ^
    "}" ^
    "$webclient.DownloadFile('%DISTRIBUTION_URL%', '%MAVEN_ZIP%')"
if ERRORLEVEL 1 goto error

powershell -Command ^
    "$ErrorActionPreference = 'Stop';" ^
    "if (-not (Test-Path -Path '%MAVEN_HOME%')) {" ^
    "    New-Item -Path '%MAVEN_HOME%' -ItemType Directory | Out-Null;" ^
    "}" ^
    "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_HOME%' -Force"
if ERRORLEVEL 1 goto error

del "%MAVEN_ZIP%" 2>nul

@REM Find the extracted Maven directory
for /D %%G in ("%MAVEN_HOME%\apache-maven-*") do set MAVEN_DIST_DIR=%%G

if exist "%MAVEN_DIST_DIR%\bin\mvn.cmd" (
    call "%MAVEN_DIST_DIR%\bin\mvn.cmd" ^
        %MAVEN_OPTS% ^
        %MAVEN_DEBUG_OPTS% ^
        "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
        %MAVEN_ARGS% ^
        %MAVEN_CONFIG% %*
    if ERRORLEVEL 1 goto error
    goto end
)

echo ERROR: Could not set up Maven distribution >&2
goto error

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

@REM pause the script if MAVEN_BATCH_PAUSE is set to 'on'
if "%MAVEN_BATCH_PAUSE%"=="on" pause

if "%MAVEN_TERMINATE_CMD%"=="on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
