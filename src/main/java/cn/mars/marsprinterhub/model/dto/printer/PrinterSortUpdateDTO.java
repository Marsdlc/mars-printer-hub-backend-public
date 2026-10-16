package cn.mars.marsprinterhub.model.dto.printer;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrinterSortUpdateDTO implements Serializable {
    /**
     * 打印机ID
     */
    private Long id;

    /**
     * 排序值
     */
    private Integer sort;
}
