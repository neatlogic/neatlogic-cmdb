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
public class SearchLegalValidApi extends PrivateApiComponentBase {

    @Resource
    private LegalValidMapper legalValidMapper;


    @Override
    public String getToken() {
        return "/cmdb/legalvalid/search";
    }

    @Override
    public String getName() {
        return "搜索合规校验规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = LegalValidVo[].class)})
    @Description(desc = "搜索合规校验规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        LegalValidVo legalValidVo = JSONObject.toJavaObject(jsonObj, LegalValidVo.class);
        return legalValidMapper.searchLegalValid(legalValidVo);
    }
}
