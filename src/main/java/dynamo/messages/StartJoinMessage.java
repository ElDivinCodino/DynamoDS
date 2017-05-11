package dynamo.messages;

import java.io.Serializable;

/**
 * Message that contains the IP address and the number of port of the coordinator that has to manage the joining request
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

    @Override
    public String toString() {
        return "StartJoinMessage{" +
                "remoteIp='" + remoteIp + '\'' +
                ", remotePort='" + remotePort + '\'' +
                '}';
    }
}
