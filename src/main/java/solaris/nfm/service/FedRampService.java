package solaris.nfm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solaris.nfm.capability.annotation.jsonvalid.JsonService;

import java.io.IOException;

@Service
@Slf4j
public class FedRampService {
    private JsonNode fedRamp;
    @Autowired
    private JsonService jsonService;

    @PostConstruct
    public void init()
    {
        try
        {
            fedRamp = jsonService.getJsonNodeFromClasspathForYaml("mitre/fedRamp.yaml");
        } catch (final IOException e){
            log.error("Can not read file mitre/fedRamp.yaml");
        }
    }

    public JsonNode getFedRamp()
    {
        return this.fedRamp;
    }
}
