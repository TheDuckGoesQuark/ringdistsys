package node.clientmessaging.messages;

import com.google.gson.Gson;

public class UserMessageEncoder implements Encoder<UserMessage, String> {

    private static final Gson gson = new Gson();

    @Override
    public String encode(UserMessage userMessage) {
        return gson.toJson(userMessage);
    }

    @Override
    public UserMessage decode(String encodedMessage) {
        return gson.fromJson(encodedMessage, UserMessage.class);
    }
}
