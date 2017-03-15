package dynamo.messages;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class StartJoinMessage implements Serializable {

    private String remoteIp;
    private String remotePort;

    public StartJoinMessage(String remoteIp, String remotePort) {
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public String getRemotePort() {
        return remotePort;
    }
}
