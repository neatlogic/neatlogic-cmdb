/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.attr;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/get";
    }

    @Override
    public String getName() {
        return "获取属性详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "属性id")})
    @Output({@Param(explode = AttrVo[].class)})
    @Description(desc = "获取属性详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return attrMapper.getAttrById(jsonObj.getLong("id"));
    }
}
