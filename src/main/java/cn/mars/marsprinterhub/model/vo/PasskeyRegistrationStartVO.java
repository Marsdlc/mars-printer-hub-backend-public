package cn.mars.marsprinterhub.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasskeyRegistrationStartVO {

    private String requestId;

    private JsonNode options;
}
