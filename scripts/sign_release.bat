@echo off
setlocal enabledelayedexpansion

rem Usage: sign_release.bat [APK_PATH] [ALIGNED_OUT] [SIGNED_OUT]

set BUILD_TOOLS_VERSION=%BUILD_TOOLS_VERSION%
if "%BUILD_TOOLS_VERSION%"=="" set BUILD_TOOLS_VERSION=33.0.2

if not defined ANDROID_SDK_ROOT if defined ANDROID_HOME set ANDROID_SDK_ROOT=%ANDROID_HOME%
if not defined ANDROID_SDK_ROOT (
  echo ANDROID_SDK_ROOT or ANDROID_HOME must be set
  exit /b 2
)

set APK=%1
if "%APK%"=="" set APK=app\build\outputs\apk\release\app-release.apk
set ALIGNED=%2
if "%ALIGNED%"=="" set ALIGNED=app\build\outputs\apk\release\app-release-aligned.apk
set SIGNED=%3
if "%SIGNED%"=="" set SIGNED=app\build\outputs\apk\release\app-release-aligned-signed.apk

set KS=%RELEASE_STORE_FILE%
if "%KS%"=="" set KS=keystores\sharebuddy-test.jks
set KSPASS=%RELEASE_STORE_PASSWORD%
if "%KSPASS%"=="" set KSPASS=testpass
set ALIAS=%RELEASE_KEY_ALIAS%
if "%ALIAS%"=="" set ALIAS=sharebuddy
set KPASS=%RELEASE_KEY_PASSWORD%
if "%KPASS%"=="" set KPASS=testpass

set BUILD_TOOLS_DIR=%ANDROID_SDK_ROOT%\build-tools\%BUILD_TOOLS_VERSION%
set ZIPALIGN=%BUILD_TOOLS_DIR%\zipalign.exe
set APKSIGNER=%BUILD_TOOLS_DIR%\apksigner.bat

echo Using SDK: %ANDROID_SDK_ROOT%
echo Using build-tools: %BUILD_TOOLS_VERSION%

if not exist "%ZIPALIGN%" (
  echo zipalign not found at %ZIPALIGN%
  exit /b 2
)
if not exist "%APKSIGNER%" (
  echo apksigner not found at %APKSIGNER%
  exit /b 2
)

if not exist "%APK%" (
  echo input APK not found: %APK%
  exit /b 2
)

echo Zipaligning: %APK% -> %ALIGNED%
"%ZIPALIGN%" -v -p 4 "%APK%" "%ALIGNED%"

echo Signing: %ALIGNED% -> %SIGNED%
"%APKSIGNER%" sign --ks "%KS%" --ks-key-alias %ALIAS% --ks-pass pass:%KSPASS% --key-pass pass:%KPASS% --out "%SIGNED%" "%ALIGNED%"

echo Verifying signature for %SIGNED%
"%APKSIGNER%" verify --print-certs "%SIGNED%"

echo Signed APK: %SIGNED%

endlocal
exit /b 0
