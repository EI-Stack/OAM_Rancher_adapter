package solaris.nfm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import solaris.nfm.service.MitreService;

@RestController
@RequestMapping("/v1")
@Validated
public class MitreCtr
{
    @Autowired
    private MitreService mitreSrv;

	/**
     * 讀取全部 FiGHT 的內容 (Json 格式)
     */
    @GetMapping(value = "/mitre/fight")
    @ResponseStatus(HttpStatus.OK)
    public JsonNode fetchJsonSchemaFile() throws Exception
    {
        return mitreSrv.getFight();
    }

}
