postie
======

*postie* is a Java library that helps You to send messages over a network connection. It takes all the work that typically delays You when coding for a network application: socket creation, thread handling and object serialization.

How to use it?
--------------

Just add the following dependency to Your `pom.xml`:

```xml
<dependency>
	<groupId>de.teiesti.postie</groupId>
	<artifactId>postie</artifactId>
	<version>0.2.0</version>
</dependency>
```

**Note:** This project is in an early developement stage. There has been no productive release yet: Anything can change without increasing the major version. (Click [here](http://semver.org/spec/v2.0.0.html) and review the first FAQ entry.)

How to contibute?
--------------------

Simply do the following:

1. Fork this repository.
2. **Code!** 
3. Create a pull request.

Version history
---------------

### Upcoming versions

- `1.0.0` First stable release. No more major API changes.
- `0.4.0` Some kind of *multiplexing* to support different message types across one Postman.
- `0.3.0` Add *Office*, a class that handles ServerSockets to spawn Postmen.

### Current version

- `0.2.0` *Postman* introduced: A *Mailbox* is now a simple *Receiver*. Pluggable serialization added.

### Previous versions

- `0.1.0` Initial approach: *Mailbox* is the central class that handles messages.
