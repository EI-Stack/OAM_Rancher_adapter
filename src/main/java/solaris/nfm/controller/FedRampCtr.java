package solaris.nfm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import solaris.nfm.service.FedRampService;
import solaris.nfm.service.MitreService;

@RestController
@RequestMapping("/v1")
@Validated
public class FedRampCtr {
    @Autowired
    private FedRampService fedRampService;

    /**
     * 讀取42組 FedRamp 的內容 (Json 格式)
     */
    @GetMapping(value = "/fedRamp")
    @ResponseStatus(HttpStatus.OK)
    public JsonNode fetchFedRampJsonSchemaFile() throws Exception
    {
        return fedRampService.getFedRamp();
    }
}
