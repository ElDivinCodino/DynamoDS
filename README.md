# Dynamo - A Distributed Key-Value Storage System

**Distributed Systems 1 project course - Master Course in Computer Science @ UniTn  2016/17**

##Authors:

**Stefano Fioravanzo** - stefano.fioravanzo@studenti.unitn.it

**Francesco Alzetta** - francesco.alzetta@studenti.unitn.it

```
git clone ...
```

---

Brief description

- how the system works, descirption of the nodes and their operations and the clients.
- The fact that they can communicate between multiuple machines etc..
- how the storage works and where it is saved
- what are the parameters, the quorums and how to set them
- type of messages etc...

## Build/Run the project

The project uses the Gradle build system to resolve its dependencies. To run and compile it just use the functions `node` and `client` present in `build.gradle`.

The syntax required by these functions to provide additional parameters to the main class is a little bit cumbersome, `run.sh` provide a simpler way to run the project by calling itself the Gradle functions.

```bash
# start the first node in the system
./run.sh node start <node_id>
# join a second node in the system
./run.sh node join <id_address> <port> <optional: node_id>

# issue a write operation to a node
./run.sh client <ip_address> <port> update <key> <value>
```

#### Fat Jars

Alternatively, you may want to create simple to use Jar packets. Since the project has two entry points, one for `Node` and one for `Client`, we have to rely on Gradle functions to compile it into two different Jars.

The functions `nodejar` and `clientjar` in `build.gradle` do just that. To compile the project into Jars file simply run:

```bash
# you will find the jar files in build/libs/
./gradlew nodejar
./gradlew clientjar

# then simply do:
java -jar node.jar param1 ... paramN
java -jar client.jar param1 ... paramN

```