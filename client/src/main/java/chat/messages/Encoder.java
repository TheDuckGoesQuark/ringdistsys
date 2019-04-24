package chat.messages;

import java.util.Optional;

public interface Encoder<FromType, ToType> {

    ToType encode(FromType obj);

    Optional<ClientMessage> decode(ToType obj);

}
