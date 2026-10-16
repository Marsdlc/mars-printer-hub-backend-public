package cn.mars.marsprinterhub.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.mars.marsprinterhub.exception.BusinessException;
import cn.mars.marsprinterhub.exception.ErrorCode;
import cn.mars.marsprinterhub.exception.ThrowUtils;
import cn.mars.marsprinterhub.model.dto.system.AliyunMarketExpressQueryResponseDTO;
import cn.mars.marsprinterhub.model.enums.LogisticsStatusEnum;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AliyunMarketExpressService {

    private static final String HOST = "https://slypass3.market.alicloudapi.com";
    private static final String QUERY_PATH = "/express3";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private SysConfigService sysConfigService;

    public AliyunMarketExpressQueryResponseDTO queryExpress(String number, String type, String mobile) {
        ThrowUtils.throwIf(StrUtil.isBlank(number), ErrorCode.PARAMS_ERROR, "快递单号不能为空");

        String appCode = sysConfigService.getAliyunMarketAppCode();

        HttpRequest request = HttpRequest.get(HOST + QUERY_PATH)
                .header("Authorization", "APPCODE " + appCode)
                .form("number", number);
        if (StrUtil.isNotBlank(type)) {
            request.form("type", type);
        }
        if (StrUtil.isNotBlank(mobile)) {
            request.form("mobile", mobile);
        }

        try (HttpResponse response = request.execute()) {

            String responseBody = response.body();
            ThrowUtils.throwIf(response.getStatus() != 200, ErrorCode.SYSTEM_ERROR, "阿里云市场快递查询失败: " + responseBody);

            AliyunMarketExpressQueryResponseDTO result = objectMapper.readValue(responseBody, AliyunMarketExpressQueryResponseDTO.class);
            ThrowUtils.throwIf(result == null, ErrorCode.SYSTEM_ERROR, "阿里云市场快递查询结果为空");
            ThrowUtils.throwIf(Boolean.FALSE.equals(result.getSuccess()), ErrorCode.SYSTEM_ERROR,
                    StrUtil.blankToDefault(result.getMsg(), "阿里云市场快递查询失败"));

            enrichStatusText(result);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用阿里云市场快递查询失败, number={}, type={}, mobile={}", number, type, mobile, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云市场快递查询失败");
        }
    }

    private void enrichStatusText(AliyunMarketExpressQueryResponseDTO result) {
        if (result == null || result.getData() == null) {
            return;
        }

        AliyunMarketExpressQueryResponseDTO.DataBody data = result.getData();
        List<AliyunMarketExpressQueryResponseDTO.ExpressInfo> infoList = data.getInfo();
        if (infoList == null || infoList.isEmpty()) {
            return;
        }

        for (AliyunMarketExpressQueryResponseDTO.ExpressInfo info : infoList) {
            info.setLogisticsStatusText(LogisticsStatusEnum.getByCode(info.getLogisticsStatus()).getText());

            List<AliyunMarketExpressQueryResponseDTO.LogisticsTraceDetail> details = info.getLogisticsTraceDetailList();
            if (details == null || details.isEmpty()) {
                continue;
            }

            for (AliyunMarketExpressQueryResponseDTO.LogisticsTraceDetail detail : details) {
                detail.setLogisticsStatusText(LogisticsStatusEnum.getByCode(detail.getLogisticsStatus()).getText());
                detail.setSubLogisticsStatusText(LogisticsStatusEnum.getByCode(detail.getSubLogisticsStatus()).getText());
            }
        }
    }
}
