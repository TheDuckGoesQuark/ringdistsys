package node;

public class ComUtil {

    public static int timeoutJitter(int defaultTimeout) {
        return (int) Math.ceil(defaultTimeout * Math.random());
    }

}
