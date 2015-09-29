INTRODUCTION

ResfulHS is Java based web-service which allows for the resolving, creation,
modifications, auditing, and deletion of handles through a RESTful interface.
It's build using the Restlet 2.0 framework (http://www.restlet.org/) given it
the ability to run as a standalone service without requiring an additional
servlet container, or webserver.


INSTALLATION

Unzip the resfulhs.zip file in the direction where you want to install it.
Edit the restfulhs.properties to set the port, and authentication information
for the Handle Servers you want to have access to. If you are using PUBKEY
authentication then you will also need to have the authentication files in a
location accessable to the program.


RUNNING

On Unix, change to installation directory and execute the following command to
run the program in the background.

nohup java -jar RestfulHS.jar restfulhs.properties &

All login information is output in the file named nohup.out

There is also a start-up script, "dib_restfulhs" which can be used to run the
service at startup or to stop it.

USING: CREATING/UPDATING HANDLES

Being a RESTful service it can be accessed using any HTTP client with access
to the host. For example using CURL and the sample XML, this is how to work
with handles. All handles are created on the developement handle server
(prefix: 10676). The element <hs:description></hs:description> is optional
and can be leftout of the input file.

<?xml version="1.0" encoding="UTF-8"?>

<hs:info xmlns:hs="info:nyu/dl/v1.0/identifiers/handle">
    <hs:binding>http://hidvl.nyu.edu/video/000568197.html</hs:binding>
    <hs:description>Test Handle</hs:description>
</hs:info>

For example, to access this service on the dlibprod, which uses an apache
mod_proxy and basic authentication, use the following commands.
Curl is used as the http client

To retrieve a handle issue :
curl --user dlts-rhs:[password] -X GET -i -k
    https://dlibprod.home.nyu.edu/id/handle/10676/testns100

To update or create a handle issue :
curl --user dlts-rhs:[password] -X PUT -H 'Content-type: text/xml' -id @testData.xml -k
    https://dlibprod.home.nyu.edu/id/handle/10676/your_handle1

To delete a handle
curl --user dlts-rhs:[password] -X DELETE -i -k
    https://dlibprod.home.nyu.edu/id/handle/10676/your_handle1

To create a handle using the Development or Production NOID minter, simply issue
the POST command with just the prefix. If the development prefix (10676) is used,
then the developement NOID minter is used, while the production minter is
used when the production prefix (2333.1) is used. The returned xml will contain
the prefix/handle that was created.

curl --user dlts-rhs:[password] -X POST -H 'Content-type: text/xml' -id @testData.xml -k
    https://dlibprod.home.nyu.edu/id/handle/10676


USING: AUDITING HANDLES

It may sometimes be nessary to run audits on a set of handles. That is, to check
to see that a not only does the handle exists, but that the url it's bound to
can be read from. To this end, an audit service is implemented which allows the
retrival of all handles for a particular prefix, or the status information for
a particular handle.  This service is also protected by basic authentication
and uses the same userid/password credentials for the other handle service.

To get a list of all handles for a particular prefix enter. You will be prompted
for a user id and password. Enter dlts-rhs for user id
https://dlibprod.home.nyu.edu/audit/handle/2333.1

To get the status of a particular handle, just add the handle to the prefix.
For, example :
https://dlibprod.home.nyu.edu/audit/handle/2333.1/5QFTTF6J

The status is checked by seeing if data can be read from the url provided.
If it can't, the status is indicated as "BROKEN", if it can then it's indicated
as "OK". If the handle doesn't exists, then the status is "NOT FOUND".

The format of the status string returned by the service is as follows
Handle \t Status \t URL \t Timestamp \t Description

ACCESS RESTRICTION

In the default setting, the service only allows connection from clients running
on the localhost to prevent unauthorize use.

By changing the "allow.connection=localhost" variable, to "allow.connection=all"
in the restfulhs.properties file, connections can be allowed from clients over
the network.

APACHE MODPROXY AND SSL

One method to allow secure access to the service is to use Apache with modproxy
and SSL as a frontend. In this case, Apache can take care of user authentication
and encryption.

See the following links on setting up a proxy in Apache.
http://httpd.apache.org/docs/2.0/mod/mod_proxy.html#proxypass
http://httpd.apache.org/docs/2.0/mod/mod_proxy.html#proxypassreverse

The contents of the apache config file (resfulhs.conf) to enable proxying and 
ssl authentication of this service is. This assumes apache has been configured
with support for mod_proxy and ssl.

<Location "/id/handle">
        RewriteEngine On
        RewriteCond %{HTTPS} off
        RewriteRule (.*) https://%{HTTP_HOST}%{REQUEST_URI}
</Location>

<Location "/audit/handle">
        RewriteEngine On
        RewriteCond %{HTTPS} off
        RewriteRule (.*) https://%{HTTP_HOST}%{REQUEST_URI}
</Location>

<Location "/id/handle">
AuthType Basic
AuthName "RESTful Handle Server Domain"
AuthUserFile /usr/local/apache/passwd/passwords
Require user dlts-rhs
</Location>

<Location "/audit/handle">
AuthType Basic
AuthName "RESTful Handle Server Domain"
AuthUserFile /usr/local/apache/passwd/passwords
Require user dlts-rhs
</Location>

ProxyRequests off
#Apache 2.0, no parameters
ProxyPass           /id/handle/  http://localhost:8082/id/handle/
ProxyPassReverse    /id/handle/  http://localhost:8082/id/handle/

ProxyPass           /audit/handle/  http://localhost:8082/audit/handle/
ProxyPassReverse    /audit/handle/  http://localhost:8082/audit/handle/

#Apache 2.2 and above
ProxyPass           /id/handle/  http://localhost:8082/id/handle/ smax=5 max=10 disablereuse=On ttl=120 retry=0
ProxyPassReverse    /id/handle/  http://localhost:8082/id/handle/

ProxyPass           /audit/handle/  http://localhost:8082/audit/handle/ smax=5 max=10 disablereuse=On ttl=120 retry=0
ProxyPassReverse    /audit/handle/  http://localhost:8082/audit/handle/


LOG FILE

All log information is placed in the nohup.out file in the install directory
of service. This file is not rotated and will need to be backup and cleaned out
every once in a while. If Apache is being used as a frontend to the service,
then apache also logs all incoming connections.

NOTE ON SCALABILITY

In the default configuration up to 10 clients can  connect simultaneously to the
service.  It takes ~ 2 second for each handle to be created with a CPU load of
less than 2%. Creation of NOID handles takes about ~4 seconds. So far, over
50,000 handles have been sequentially created and deleted with no problem.

  
SOURCE CODE

The current source code can be found at:
https://v1.home.nyu.edu/svn/dlib/pr/handle-rest/trunk/