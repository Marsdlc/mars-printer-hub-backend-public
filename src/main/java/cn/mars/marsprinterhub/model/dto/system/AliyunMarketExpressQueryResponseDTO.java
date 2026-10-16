package cn.mars.marsprinterhub.model.dto.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliyunMarketExpressQueryResponseDTO {

    private String msg;

    private Boolean success;

    private Integer code;

    private DataBody data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBody {
        private String orderNo;
        private List<ExpressInfo> info;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExpressInfo {
        private String cpCode;
        private String theLastTime;
        private String mailNo;
        private String theLastMessage;
        private String cpMobile;
        private String logisticsCompanyName;
        private String cpUrl;
        private String takeTime;
        private String logisticsStatusDesc;
        private String logisticsStatus;
        private String logisticsStatusText;
        private List<LogisticsTraceDetail> logisticsTraceDetailList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogisticsTraceDetail {
        private String areaCode;
        private String areaName;
        private String subLogisticsStatus;
        private Long time;
        private String timeDesc;
        private String logisticsStatus;
        private String logisticsStatusText;
        private String subLogisticsStatusText;
        private String desc;
    }
}
