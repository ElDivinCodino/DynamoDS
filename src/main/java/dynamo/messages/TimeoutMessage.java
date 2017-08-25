package dynamo.messages;

import java.io.Serializable;

/**
 * Message useful to inform the receiver that the operation it requested has not been successful, due to a timeout
 */
public class TimeoutMessage implements Serializable{
    private boolean init = false;

    public TimeoutMessage(boolean init) {
        this.init = init;
    }

    public boolean isInit() {
        return init;
    }

    @Override
    public String toString() {
        return "TimeoutMessage{" +
                "init=" + init +
                '}';
    }
}
