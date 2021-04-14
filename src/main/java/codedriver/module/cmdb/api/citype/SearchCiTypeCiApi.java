/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.citype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiTypeCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/citype/search";
    }

    @Override
    public String getName() {
        return "获取模型类型和模型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
        @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "获取模型类型和模型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ciVo = JSONObject.toJavaObject(jsonObj, CiVo.class);
        return ciMapper.searchCiTypeCi(ciVo);
    }
}
