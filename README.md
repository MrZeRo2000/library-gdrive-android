# library-gdrive-android

# Generate debug cert hash:
C:\WinApp\jdk-12.0.2\bin\keytool -exportcert -alias androiddebugkey -keystore %HOMEPATH%\.android\debug.keystore | C:\cygwin64\bin\openssl sha1 -binary | C:\cygwin64\bin\openssl base64
