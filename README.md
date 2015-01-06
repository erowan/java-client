Stub-O-Matic Client
===================

Java language binding

A Maven project POM file is provided to build the library::

       $ mvn clean package install

ECLIPSE IDE
===========

To integrate with Eclipse IDE perform the following 

If you don't already have an eclipse M2_REPO defined::

       $ mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo

Create eclipse project using Maven:: 

       $ mvn -Declipse.workspace=<eclipse-workspace> eclipse:configure-workspace  
       $ mvn eclipse:eclipse -DdownloadSources=true

TESTING
=======

You can run the test suite by running the following:: 

        $ mvn test

Use the ``-Dtest.url`` flag to point to your own stubo server (defaults to localhost:8001)

        $ mvn -Dtest.url=my-stubo-server.com test 
