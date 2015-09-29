# RestfulHS 1.0

ResfulHS is Java based web-service which allows for the resolving, creation,
modifications, auditing, and deletion of handles through a RESTful interface.
It's build using the Restlet 2.0 framework (http://www.restlet.org/) given it
the ability to run as a standalone service without requiring an additional
servlet container, or webserver.

###INSTALLATION

Unzip the resfulhs.zip file in the direction where you want to install it.
Edit the restfulhs.properties to set the port, and authentication information
for the Handle Servers you want to have access to. If you are using PUBKEY
authentication then you will also need to have the authentication files in a
location accessable to the program.


###RUNNING

On Unix, change to installation directory and execute the following command to
run the program in the background.

nohup java -jar RestfulHS.jar restfulhs.properties &

All login information is output in the file named nohup.out

There is also a start-up script, "dib_restfulhs" which can be used to run the
service at startup or to stop it.