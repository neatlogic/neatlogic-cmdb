/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.legalvalid.LegalValidVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
@Transactional
public class GetIllegalCiEntityCountApi extends PrivateApiComponentBase {
    @Resource
    private IllegalCiEntityMapper illegalCiEntityMapper;

    @Override
    public String getName() {
        return "获取不合规配置项数量";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = LegalValidVo[].class)})
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        return illegalCiEntityMapper.getIllegalCiEntityCountByCiId(ciId);
    }

    @Override
    public String getToken() {
        return "/cmdb/cientity/illegal/count";
    }
}
