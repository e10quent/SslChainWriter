## Usage with srcclr Windows agent in PowerShell

After running `srcclr scan` for the first time, if you receive an error indicating an untrusted certificate root/intermediate you can use the srcclr built in Java JRE to run this tool as follows:

```pwsh
PS C:\Users\FSengil> cd $Env:TEMP\srcclr\<some-number>\srcclr-<version>\jre\bin
PS C:\Users\FSengil\AppData\Local\Temp\srcclr\1528\srcclr-3.4.17\jre\bin> .\java -version
openjdk version "11.0.2" 2019-01-15 LTS
OpenJDK Runtime Environment Zulu11.29+11-CA (build 11.0.2+9-LTS)
OpenJDK 64-Bit Server VM Zulu11.29+11-CA (build 11.0.2+9-LTS, mixed mode)
PS C:\Users\FSengil\AppData\Local\Temp\srcclr\1528\srcclr-3.4.17\jre\bin> .\java -jar <path-to>\CertChainWriter.jar api.sourceclear.io 443
-----BEGIN CERTIFICATE-----
MIIESTCCAzGgAwIBAgITBn+UV4WH6Kx33rJTMlu8mYtW...
```

Proxy support is also provided:

```pwsh
PS C:\...\srcclr-3.4.17\jre\bin> .\java -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=8888 -jar <path-to>\CertChainWriter.jar api.sourceclear.io 443
```

You can then import the certificate files into the keystore (default password is `changeit`):

```pwsh
PS C:\Users\FSengil\AppData\Local\Temp\srcclr\1528\srcclr-3.4.17\jre\bin> .\keytool.exe -import -alias cert1 -keystore ..\lib\security\cacerts -file cert1.pem
Warning: use -cacerts option to access cacerts keystore
Enter keystore password:
Certificate was added to keystore
```
