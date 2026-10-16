package cn.mars.marsprinterhub.homeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class HomeAssistantEntityState {

    @JsonProperty("entity_id")
    private String entityId;

    private String state;

    private Map<String, Object> attributes;

    @JsonProperty("last_changed")
    private String lastChanged;

    @JsonProperty("last_reported")
    private String lastReported;

    @JsonProperty("last_updated")
    private String lastUpdated;

    private Map<String, Object> context;
}
