package solaris.nfm.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "server.open-daylight.http")
public class OdlInfo {
    private String url;
    private String host;
    private int port;
    private String username;
    private String password;
}