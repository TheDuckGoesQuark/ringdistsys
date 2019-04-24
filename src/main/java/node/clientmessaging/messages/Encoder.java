package node.clientmessaging.messages;

public interface Encoder<T1, T2> {

    public T2 encode(T1 obj);

    public T1 decode(T2 encodedObj);

}
