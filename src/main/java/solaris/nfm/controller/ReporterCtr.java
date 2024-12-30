package solaris.nfm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import solaris.nfm.service.ReportService;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;


@RestController
@RequestMapping("/v1/reporter")
@Slf4j
public class ReporterCtr {

    @Autowired
    private ReportService reportService;

    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    public void createReporter(@RequestBody final JsonNode input) throws Exception {
        if (input == null) {
            reportService.sendReportToAdmin();
            return;
        }
        JsonNode date = input.get("date");
        if (date == null) {
            reportService.sendReportToAdmin();
        } else {
            reportService.sendReportToAdmin(LocalDate.parse(date.asText()));
        }

    }
}
