SSOLoginWebApp
====================

![Build Status](https://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-SSOLoginWebApp) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-SSOLoginWebApp/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-SSOLoginWebApp)

The web-frontend for the Whydah SSO services. It uses SecurityTokenService and UserIdentityBackend
behind the scenes, where SecurityTokenService needs to be accessible from the same zone that your applications reside.

![Architectural Overview](https://raw.githubusercontent.com/cantara/Whydah/master/images/Whydah%20infrastructure.png)


Start/Install
=============

* create a user for the service


* create update-service.sh
```
#!/bin/sh

A=SSOLoginWebApp
V=SNAPSHOT


if [[ $V == *SNAPSHOT* ]]; then
   echo Note: If the artifact version contains "SNAPSHOT" - the artifact latest greates snapshot is downloaded, Irrelevent of version number!!!
   path="http://mvnrepo.cantara.no/content/repositories/snapshots/net/whydah/sso/$A"
   version=`curl -s "$path/maven-metadata.xml" | grep "<version>" | sed "s/.*<version>\([^<]*\)<\/version>.*/\1/" | tail -n 1`
   echo "Version $version"
   build=`curl -s "$path/$version/maven-metadata.xml" | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
   JARFILE="$A-$build.jar"
   url="$path/$version/$JARFILE"
else #A specific Release version
   path="http://mvnrepo.cantara.no/content/repositories/releases/net/whydah/sso/$A"
   url=$path/$V/$A-$V.jar
   JARFILE=$A-$V.jar
fi

# Download
echo Downloading $url
wget -O $JARFILE -q -N $url


#Create symlink or replace existing sym link
if [ -h $A.jar ]; then
   unlink $A.jar
fi
ln -s $JARFILE $A.jar
```


* create ssologinwebapp.TEST.properties

```
DEFCON=5
# Normal operations
applicationname=SSOLoginWebApplication
applicationid=15
applicationsecret=33779936R6Jr47D4Hj5R6p9qT
#
#securitytokenservice=http://myserver.net/tokenservice/
securitytokenservice=http://localhost/tokenservice/
#useridentitybackend=http://myserver.net/uib/
useridentitybackend=http://localhost/uib/


#myuri=http://myserver.net/sso/
myuri=http://localhost:9997/sso/

cookiedomain=.whydah.net

logintype.facebook=false
logintype.openid=false
logintype.omni=false
logintype.userpassword=enabled
logintype.netiq=enabled
logintype.netiq.text=NetIQ
logintype.netiq.logo=images/netiqlogo.png
# verification rules for NetIQ redirect control .field=value (which should substring match
logintype.netiq.header.X-Forwarded-For=127.0.0.1


signupEnabled=false


netIQauthURL=https://netiq.novel.com/

logourl=http://stocklogos.com/sites/default/files/styles/logo-medium/public/logos/image/dc5f9f951e37c6ebb2ebecb619fe7555.png

appLinks={'appName':'appUrl','appName2':'appUrl2'}
```
```


* create start-service.sh

```
#!/bin/sh
nohup /usr/bin/java -DIAM_MODE=PROD -DIAM_CONFIG=/home/SSOLoginWebApp/ssologinservice.PROD.properties -jar /home/SSOLoginWebApp/SSOLoginWebApp.jar
```


Typical apache setup
====================

```
<VirtualHost *:80>
        ServerName myserver.net
        ServerAlias myserver
        ProxyRequests Off
        <Proxy *>
                Order deny,allow
                Allow from all
        </Proxy>
        ProxyPreserveHost on
                ProxyPass /sso http://localhost:9997/sso
                ProxyPass /uib http://localhost:9995/uib
                ProxyPass /tokenservice http://localhost:9998/tokenservice
                ProxyPass /useradmin http://localhost:9996/useradmin
                ProxyPass /test http://localhost:9990/test/
</VirtualHost>
```




Developer info
==============

* https://wiki.cantara.no/display/iam/Architecture+Overview
* https://wiki.cantara.no/display/iam/Key+Whydah+Data+Structures
* https://wiki.cantara.no/display/iam/Modules

If you are planning on integrating, you might want to run SecurityTokenService in DEV mode. This shortcuts the authentication.
You can manually control the UserTokens for the different test-users you want, by creating a file named t_<username>.token which
consists of the XML representation of the access roles++ you want the spesific user to expose to the integrated application.



## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
