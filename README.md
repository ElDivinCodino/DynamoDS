# dynamo



### Messages

ActorSystem to Node:

- StartJoinMessage: Tell to node that it has to join the system with given remote reference

Node to Node:

- PeerListMessage (Request/Response): A node asks for the list of actors present in the system
- RequestInitItemsMessage (Request/Response): A new node asks to its clockwise neighbor for the data it is responsible for

- 
