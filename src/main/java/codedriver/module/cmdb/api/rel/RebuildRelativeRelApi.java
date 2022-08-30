/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.rel;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.relativerel.RelativeRelManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class RebuildRelativeRelApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/rel/relative/rebuild";
    }

    @Override
    public String getName() {
        return "重建级联关系数据";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({@Param(name = "relId", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "重建级联关系数据接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("relId");
        RelativeRelManager.rebuild(relId);
        return null;
    }

}
