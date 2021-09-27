/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.legalvalid;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.legalvalid.LegalValidVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetLegalValidApi extends PrivateApiComponentBase {

    @Resource
    private LegalValidMapper legalValidMapper;


    @Override
    public String getToken() {
        return "/cmdb/legalvalid/get";
    }

    @Override
    public String getName() {
        return "获取合规校验规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")})
    @Output({@Param(explode = LegalValidVo.class)})
    @Description(desc = "获取合规校验规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return legalValidMapper.getLegalValidById(jsonObj.getLong("id"));
    }
}
