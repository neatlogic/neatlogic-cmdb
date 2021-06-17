/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.customview.CustomViewCiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.exception.customview.CustomViewCiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
import codedriver.module.cmdb.service.customview.CustomViewService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCustomViewDataApi extends PrivateApiComponentBase {

    @Resource
    private CustomViewDataService customViewDataService;

    @Resource
    private CustomViewService customViewService;

    @Override
    public String getName() {
        return "根据配置项id获取自定义视图id数据";
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/data/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "视图id", isRequired = true),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
    })
    @Output({@Param(explode = CustomViewVo.class)})
    @Description(desc = "根据配置项id获取自定义视图id数据接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(paramObj, CustomViewConditionVo.class);
        customViewConditionVo.setCustomViewId(paramObj.getLong("id"));
        Map<String, List<String>> columnMap = new HashMap<>();
        Map<String, List<Map<String, String>>> resultListMap = new HashMap<>();
        CustomViewVo customViewVo = customViewService.getCustomViewDetailById(customViewConditionVo.getCustomViewId());
        customViewConditionVo.setFieldList(customViewVo.getCiList().stream().map(CustomViewCiVo::getUuid).collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(customViewConditionVo.getFieldList())) {
            List<Map<String, Long>> resultList = customViewDataService.getCustomViewCiEntityIdById(customViewConditionVo);
            for (Map<String, Long> result : resultList) {
                for (String key : result.keySet()) {
                    String ciUuid = key.replace("_id", "");
                    CustomViewCiVo ciVo = customViewVo.getCustomCiByUuid(ciUuid);
                    if (ciVo != null) {
                        ciVo.addCiEntityId(result.get(key));
                    }
                }
            }
        } else {
            throw new CustomViewCiNotFoundException();
        }
        return customViewVo;
    }


}
