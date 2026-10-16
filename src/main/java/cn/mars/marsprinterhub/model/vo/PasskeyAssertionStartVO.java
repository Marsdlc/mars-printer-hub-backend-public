package cn.mars.marsprinterhub.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasskeyAssertionStartVO {

    private String requestId;

    private JsonNode options;
}
