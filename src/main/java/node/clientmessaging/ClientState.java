package node.clientmessaging;

import node.clientmessaging.messagequeue.UserMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class ClientState {

    private Map<User, Queue<UserMessage>> clientMessageQueues = new HashMap<>();


}
