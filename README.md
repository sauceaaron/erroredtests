# erroredtests

To build a jar file, type `mvn clean package`

To execute the jar file, type `cd target; java -jar errored-tests-1.0.jar`

To just execute from source, type `mvn exec:java -Dexec.mainClass="com.saucelabs.api.GetErroredTests"`

You can pass the following options as properties:

* USERNAME (your Sauce Labs username) default will check for environment variable SAUCE_USERNAME)=
* ACCESS_KEY (your Sauce Labs access key) default will check for environment variable SAUCE_ACCESS_KEY)
* TIME_RANGE (-1d, -1h, -1m, -1s; maximum -29d) default is "-1d"
* SCOPE (me, organization, single) default is "me"
* STATUS (errored, complete, passed, failed) defailt is "errored"

It checks for the following error strings:

* "Test did not see a new command"
* "Internal Server Error"
* "Infrastructure Error"

You can add or modify errors by editing the source code.

Probably what you want to do is something like this:
```
mvn clean package
cd target
java -jar errored-tests-1.0.jar -DTIME_RANGE=-29 -DSCOPE=organization -DSTATUS=errored -DUSERNAME=myusername -DACCESS_KEY=myaccesskey
```
