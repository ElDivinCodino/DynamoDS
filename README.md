# Dynamo



### Messages

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
    - Logging
    - RemotePaths (send to self with path)

- FRA
    - ~~Merge Client and ClientActor (use Future pattern)~~
    - Review TODOs in NodeActor's onReceive and extend Storage class accordingly
    

### MAIN CHANGES

- FRA 
    - I've changed the constructor in RequestInitItemsMessage, allowing it to take an ArrayList also in request. This because this way I can maintain a reference to 
    the Storage of the sender, doing the set difference of the Store of the newer w.r.t. the one of the older.
    This way I could complete the "RequestInitItemsMessage" case. Please check if my idea is faulty or not (if I didn't take into consideration some borderline cases).
    
    