# Dynamo - A Distributed Key-Value Storage System

**Distributed Systems 1 project course - Master Course in Computer Science @ UniTn  2016/17**

## Authors:

**Stefano Fioravanzo** - stefano.fioravanzo@studenti.unitn.it

**Francesco Alzetta** - francesco.alzetta@studenti.unitn.it

```bash
git clone https://github.com/StefanoFioravanzo/dynamo.git
```

---

In this project, we have implemented a DHT-based peer-to-peer key-value storage service inspired by Amazon Dynamo. The system consists of multiple storage nodes (just nodes hereafter) and provides a simple user interface to upload/request data and issue management commands.
The stored data is partitioned among the nodes to balance the load. The partitioning is based on the keys that are associated with both the stored items and the nodes. The keys form a circular space or “ring” (i.e. the largest key value wraps around to the smallest key value like minutes on analog clocks). A data item with the key K should be stored by the first N nodes in the clockwise direction from K on the ring, where N is a system parameter that defines the degree of replication.
When nodes leave or join the network, the system repartitions the data items accordingly.

Every node provides the data and the management services to clients (user applications). The data service consists of two commands: `update(key, value)` and `get(key)->value`. Any node in the network is able to fulfil both requests regardless of the key, forwarding data to/from appropriate nodes. The management service consists of a single leave command that requests the node to leave the network.

#### Quorums

To implement replication, the system relies on quorums and versions that are associated internally with every data item. System-wide parameters W and R specify the write and read quorums respectively (W + R > N ).

**Read Operation**: Upon receiving a get command from the client, the coordinator requests the item from the N responsible nodes. As soon as R replies arrive, it sends the data item with the highest received version back to the client.

**Write Operation**: When a node receives an update request from the client, it first requests the currently stored version of the data item from the N nodes that have its replica. As soon as Q = max(R, W ) replies have been received, the most recent stored version can be reliably determined because of the read quorum and the item can be reliably updated because of the write quorum.

The quorum values can be set in the `src/main/resources/application.conf` configuration file.

#### Storage

Local storage. Every node should maintains a persistent storage (text file) containing the key, the version and the value for every data item the node is responsible for. The location of the storage is determined by the `storage.location` parameter in the configuration file.

#### Network Communication

The Nodes can communicate in a distributed fashion, just start a new node using another node's remote IP as entry point. More on how to start the nodes below.

#### Recovery

The system also simulates crash and recovery. If you want to manually crash a node, just press Ctrl+C. The use a client to specifically recover the crashed node, details on the specific arguments to use below.

## Build/Run the project

The project uses the Gradle build system to resolve its dependencies. To run and compile it just use the functions `node()` and `client()` in `build.gradle`.

The syntax required by these functions to provide additional parameters to the main class is a little bit cumbersome, `run.sh` provide a simpler way to run the project by calling itself the Gradle functions.

```bash
# start the first node in the system
./run.sh node start <node_id>
# join a second node in the system
./run.sh node join <id_address> <port> <optional: node_id>

# issue a write operation to a node
./run.sh client <ip_address> <port> update <key> <value>
```

#### Node Arguments

...

#### Client Arguments

...

#### Fat Jars

Alternatively, you may want to create simple to use Jar packets. Since the project has two entry points, one for `Node` and one for `Client`, we have to rely on Gradle functions to compile it into two different Jars.

The functions `nodejar()` and `clientjar()` in `build.gradle` do just that. To compile the project into Jars file simply run:

```bash
# you will find the jar files in build/libs/
./gradlew nodejar
./gradlew clientjar

# then simply do:
java -jar node.jar param1 ... paramN
java -jar client.jar param1 ... paramN

```