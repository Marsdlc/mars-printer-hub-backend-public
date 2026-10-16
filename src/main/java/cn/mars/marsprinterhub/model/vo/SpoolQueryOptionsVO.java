package cn.mars.marsprinterhub.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpoolQueryOptionsVO {

    /**
     * 耗材类型
     */
    private List<String> materialTypes;
    /**
     * 耗材品牌
     */
    private List<String> brand;
    /**
     * 耗材外观
     */
    private List<Integer> appearance;
}
