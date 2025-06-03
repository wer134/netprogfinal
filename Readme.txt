P2P Chat System (Peer-to-Peer Chat System)
1. Project Overview
This project implements a simple Peer-to-Peer (P2P) chat system using Java. It utilizes a central RegisterServer to manage user information, while actual chat communication occurs directly between ChatClient instances in a P2P fashion. The system leverages both TCP and UDP network protocols to provide reliable communication and efficient real-time status notifications.

2. Key Features
User Registration and Discovery (RegisterServer):
Users can register (REGISTER) their ID, IP, and port information with the RegisterServer.
Other users' IP and port information can be queried (QUERY) by their ID.
Users can explicitly deregister (DEREGISTER), or their registration will be automatically removed if they are inactive for a certain period (30 seconds).
P2P Chatting (ChatClient & ChatServer):
After discovering a peer's information, a ChatClient establishes a direct TCP connection with the peer's ChatServer to initiate a chat.
Message sending and receiving are handled concurrently by separate threads.
Real-time User Status Notifications (UDP):
When a user logs in, logs out (explicitly or due to inactivity), the RegisterServer sends real-time notifications via UDP to all other online ChatClients.
ChatClients receive these UDP notifications and display relevant messages to the user.
Heartbeat (UDP):
Each ChatClient periodically (every 10 seconds) sends a heartbeat message via UDP to the RegisterServer to indicate its active status. The RegisterServer uses this information to manage user activity.
Chat Message Logging:
All chat messages sent through the system are logged to a file (chat_log.txt) on the server side.
3. System Components
The system consists of the following main components:

RegisterServer.java:
TCP (8888): Handles user registration, query, deregistration, and message logging requests.
UDP (8889): Receives client heartbeats and sends user status change notifications.
Manages user information using a ConcurrentHashMap for thread-safe operations.
Includes logic for automatically removing inactive users.
ChatClient.java:
Provides the user interface and communicates with the RegisterServer via TCP.
Establishes direct TCP connections with other ChatClients' ChatServers for P2P chat.
Periodically sends UDP heartbeats to the RegisterServer.
Receives UDP notifications from the RegisterServer on its assigned port.
ChatServer.java:
Runs as an independent thread within each ChatClient instance.
Accepts incoming TCP connections from other ChatClients for P2P chat and displays received messages.
UserInfo.java:
A data model storing user information (ID, IP, Port, and last active timestamp).
MessageLog.java:
A utility class for logging chat messages to a file in a synchronized manner.
4. How to Run
Prerequisites:

Java Development Kit (JDK) 8 or later must be installed.
Compile all .java files:
Navigate to the project directory in your terminal and compile all source files:

Bash

javac *.java
Start the RegisterServer:
Open a terminal and run the RegisterServer first:

Bash

java RegisterServer
You should see messages like "RegisterServer (TCP) is listening on port 8888..." in the server console.

Start ChatClients (at least two):
Open new terminal windows (or command prompts) for each ChatClient you wish to run. You will need at least two ChatClient instances to establish a chat.

Bash

java ChatClient
Enter your ID: Provide a unique ID for each client (e.g., user1, user2).
Enter your port number for chatting and notifications: Enter a port number for each client (e.g., 5000, 5001). This port will be used by the client's ChatServer to listen for incoming connections and also to receive UDP notifications.
Example:

Bash

# First client (Terminal 1)
java ChatClient
Enter your ID: user1
Enter your port number for chatting and notifications: 5000

# Second client (Terminal 2)
java ChatClient
Enter your ID: user2
Enter your port number for chatting and notifications: 5001
Start Chatting:

After successfully registering with the RegisterServer, the ChatClient will prompt "Enter target ID: ".
Enter the ID of the peer you wish to chat with.
If the peer is online and their ChatServer is running, a connection will be established, and you can start typing messages.
Type your message and press Enter to send.
Type exit and press Enter to end the chat and terminate the client application. (The client will automatically deregister from the RegisterServer upon exit.)
5. Project Structure (Simplified)
.
├── ChatClient.java
├── ChatServer.java
├── MessageLog.java
├── RegisterServer.java
└── UserInfo.java
6. Improvements Implemented
Login/Logout/Inactivity Notifications: Added real-time status notifications via UDP to enhance user experience.
Heartbeat Mechanism: Implemented a heartbeat mechanism to periodically verify client activity, automatically removing inactive users to maintain an accurate user list on the RegisterServer.
Concurrency Handling: Utilized ConcurrentHashMap for user management in RegisterServer to safely handle multiple concurrent requests.
Graceful Shutdown: Enhanced shutdown() methods and finally blocks in client and server components to prevent resource leaks and ensure safe program termination.
Robust Error Handling: Improved exception handling for socket communication and file I/O operations to increase overall system stability.
7. Future Enhancements
GUI Interface: Replace the current console-based interface with a more user-friendly graphical interface using Swing or JavaFX.
Group Chat: Implement functionality for multiple users to participate in a single chat room.
File Transfer: Add capabilities to send and receive files (images, documents, etc.) between chat participants.
Security: Introduce message encryption and stronger user authentication (e.g., passwords).
NAT Traversal: Explore solutions like STUN/TURN servers to enable communication between clients behind Network Address Translators (NATs).
