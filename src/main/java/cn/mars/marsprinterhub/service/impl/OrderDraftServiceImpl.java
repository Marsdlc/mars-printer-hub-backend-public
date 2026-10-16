package cn.mars.marsprinterhub.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.mars.marsprinterhub.model.dto.order.OrderQuoteDraftDTO;
import cn.mars.marsprinterhub.service.OrderDraftService;
import cn.mars.marsprinterhub.service.UserService;
import cn.mars.marsprinterhub.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.mars.marsprinterhub.constant.RedisConstants.USER_ORDER_QUOTE_DRAFT_KEY;

@Service
public class OrderDraftServiceImpl implements OrderDraftService {

    private static final int MAX_DRAFT_SIZE = 20;

    private static final long DRAFT_TTL_SECONDS = 14L * 24 * 60 * 60;

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public void saveDraft(OrderQuoteDraftDTO draft) {
        Long userId = userService.getCurrentUserId();
        String key = USER_ORDER_QUOTE_DRAFT_KEY + userId;
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        draft.setId(snowflake.nextId());

        List<OrderQuoteDraftDTO> list = getDraftListInternal(key);

        // 同 ID 覆盖 + unshift
        list.removeIf(item -> Objects.equals(item.getId(), draft.getId()));
        list.add(0, draft);

        if (list.size() > MAX_DRAFT_SIZE) {
            list = new ArrayList<>(list.subList(0, MAX_DRAFT_SIZE));
        }

        redisUtils.set(key, JSONUtil.toJsonStr(list), DRAFT_TTL_SECONDS);
    }

    @Override
    public List<OrderQuoteDraftDTO> listDraft() {
        Long userId = userService.getCurrentUserId();
        return getDraftListInternal(USER_ORDER_QUOTE_DRAFT_KEY + userId);
    }

    @Override
    public void deleteDraft(String id) {
        Long userId = userService.getCurrentUserId();
        String key = USER_ORDER_QUOTE_DRAFT_KEY + userId;

        List<OrderQuoteDraftDTO> filtered = getDraftListInternal(key).stream()
                .filter(item -> !Objects.equals(item.getId(), Long.parseLong(id)))
                .collect(Collectors.toList());

        redisUtils.set(key, JSONUtil.toJsonStr(filtered), DRAFT_TTL_SECONDS);
    }

    private List<OrderQuoteDraftDTO> getDraftListInternal(String key) {
        Object value = redisUtils.get(key);
        String json = Convert.toStr(value);

        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        return JSONUtil.parseArray(json).toList(OrderQuoteDraftDTO.class);
    }
}