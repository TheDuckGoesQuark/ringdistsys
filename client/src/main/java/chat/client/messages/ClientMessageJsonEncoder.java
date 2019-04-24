package chat.client.messages;

import com.google.gson.Gson;

import java.util.Optional;

public class ClientMessageJsonEncoder implements Encoder<ClientMessage, String> {

    private static final Gson gson = new Gson();

    @Override
    public String encode(ClientMessage obj) {
        return gson.toJson(obj);
    }

    @Override
    public Optional<ClientMessage> decode(String obj) {

        // First determine message type from standard header
        final ClientMessageType type = gson.fromJson(obj, ClientMessage.class).getMessageType();

        // Parse full object based on message type
        switch (type) {
            case CHAT_MESSAGE:
                return Optional.of(gson.fromJson(obj, ChatMessage.class));
            case LEAVE_GROUP:
                return Optional.of(gson.fromJson(obj, LeaveGroupMessage.class));
            case JOIN_GROUP:
                return Optional.of(gson.fromJson(obj, JoinGroupMessage.class));
            case LOGIN:
                return Optional.of(gson.fromJson(obj, LoginMessage.class));
            case ERROR:
                return Optional.of(gson.fromJson(obj, ErrorMessage.class));
            case ALIVE:
                return Optional.of(gson.fromJson(obj, ClientMessage.class));
            default:
                return Optional.empty();
        }
    }
}
