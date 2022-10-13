/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiUniqueRuleApi extends PrivateApiComponentBase {

    @Autowired
    private CiService ciService;

    @Override
    public String getToken() {
        return "/cmdb/ciunique/save";
    }

    @Override
    public String getName() {
        return "保存模型唯一规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY,
                    desc = "属性id列表")})
    @Description(desc = "保存模型唯一规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        JSONArray attrList = jsonObj.getJSONArray("attrIdList");
        List<Long> attrIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (int i = 0; i < attrList.size(); i++) {
                attrIdList.add(attrList.getLong(i));
            }
        }
        ciService.updateCiUnique(ciId, attrIdList);
        return null;
    }

}
