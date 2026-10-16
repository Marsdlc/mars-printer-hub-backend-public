package cn.mars.marsprinterhub.controller;

import cn.mars.marsprinterhub.common.BaseResponse;
import cn.mars.marsprinterhub.model.vo.HomeVO;
import cn.mars.marsprinterhub.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.mars.marsprinterhub.common.ResultUtils.success;

/**
 * 主页接口
 *
 * @author mars
 */
@RestController
@RequestMapping("/home")
@Slf4j
@Tag(name = "Home", description = "主页相关接口")
public class HomeController {

    @Resource
    private HomeService homeService;

    @GetMapping("/summary")
    @Operation(summary = "获取首页汇总信息")
    public BaseResponse<HomeVO> getHomeSummary() {
        return success(homeService.getHomeSummary());
    }
}
