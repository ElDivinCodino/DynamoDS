# Dynamo



## Messages

ActorSystem to Node:

- **StartJoinMessage**: Tell to node that it has to join the system with given remote reference

Node to Node:

- **PeerListMessage** (Request/Response): A node asks for the list of actors present in the system
- **RequestInitItemsMessage** (Request/Response): A new node asks to its clockwise neighbor for the data it is responsible for

- **HelloMatesMessage**: A node has received the list of Nodes present in the system and the data it is responsible for, so it announces itself to the community
- **ByeMatesMessage**: tell nodes that the sender wants to leave the system. Have to replicate the data it was storing accordingly.

- **OperationMessage**: This message is used for all read/write operation between client and nodes. Putting all the logic into a single message results in a better organized code.
    
 

### TODOs

- STE
    - ~~Logging~~
    - ~~RemotePaths (send to self with path)~~
    - Finish remote communication with client

- FRA
    - ~~Merge Client and ClientActor (use Future pattern)~~
    - Review TODOs in NodeActor's onReceive and extend Storage class accordingly
    

### MAIN CHANGES

- FRA 
    - I've changed the constructor in RequestInitItemsMessage, allowing it to take an ArrayList also in request. This because this way I can maintain a reference to 
    the Storage of the sender, doing the set difference of the Store of the newer w.r.t. the one of the older.
    This way I could complete the "RequestInitItemsMessage" case. Please check if my idea is faulty or not (if I didn't take into consideration some borderline cases).
    Modified for this purpose also requestItemsToNextNode() method, see comments in the code.
    
    
### REMOTE COMMUNICATION

For the moment, the first node of the system is started as `Node start id` where id is its key. Then each new node is stared in the normal way, clearly the second will be given the port and address of the first node.

Moreover, for now each node get its unique id as follows: `Node join remote_ip remote_port local_id` so as last argument when running the application. We can decide to change this in the future, but for now it is handy so we can better control what ids to use.

All port are defined as 10000 + id, so for a node with id=15 it will have port 10015

### BUILD.GRADLE

```/**
 * Custom Gradle task to run the Client CLI.
 * You can run the application in this way: ./gradlew node -Pmyargs="arg1 arg2 ..."
 */
task node(type: JavaExec) {
    classpath sourceSets.main.runtimeClasspath
    jvmArgs = ['-ea']
    main = 'dynamo.Node'

    // this way we can pass some parameters to the java app
    if (project.hasProperty('myargs')) {
        args(myargs.split(' '))
    }
}

/**
 * Custom Gradle task to run the Client CLI.
 * You can run the application in this way: ./gradlew client -Pmyargs="arg1 arg2 ..."
 */
task client(type: JavaExec) {
    classpath sourceSets.main.runtimeClasspath
    jvmArgs = ['-ea']
    main = 'dynamo.Client'

    // this way we can pass some parameters to the java app
    if (project.hasProperty('myargs')) {
        args(myargs.split(' '))
    }
}
```

In order to run and compile the project form command line a few modifications to the `gradle.build` file have to be made. Add the above tasks to the file in order to execute from CLI the application. 

The commands to run it are specified in the comments.

Also, in order to compile successfully, you need to add a second compile group:

`compile group: 'com.typesafe.akka', name: 'akka-remote_2.11', version: '2.4.17'`

to include akka's remote communication utilities.

## REPLICATION CONSTANTS

For now I am loading the replication and quorum constants from the configuration file `application.conf`. See the code in Node.java.