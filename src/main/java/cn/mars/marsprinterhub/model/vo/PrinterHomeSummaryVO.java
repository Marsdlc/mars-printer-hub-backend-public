package cn.mars.marsprinterhub.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrinterHomeSummaryVO {

    private Long id;

    private String name;

    private String model;

    private String deviceId;

    private String status;

    private Integer mcPercent;

    private Integer remainingTime;

    private Integer sort;
}
