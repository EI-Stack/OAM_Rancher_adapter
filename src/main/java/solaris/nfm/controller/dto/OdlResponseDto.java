package solaris.nfm.controller.dto;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import solaris.nfm.util.NamingUtil;

@Data
public class OdlResponseDto {
    private boolean success = false;
    private Integer errorCode = 0;
    private String errorMessage = "success";
    private JsonNode content;

    public void setErrorCode(Integer errorCode) {
        this.success = false;
        this.errorCode = errorCode;
    }

    public void setContent(JsonNode content) {
        this.success = true;
        if (content != null && !content.isEmpty()) {
            this.content = NamingUtil.xmlTagNameConvertToCamelCase(content);
        }
    }
}