/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiNameAttrIdApi extends PrivateApiComponentBase {

    @Autowired
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/ci/savenameattr";
    }

    @Override
    public String getName() {
        return "保存模型名称属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", isRequired = true, type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "属性id")
    })
    @Description(desc = "保存模型名称属性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long attrId = jsonObj.getLong("attrId");
        CiVo ciVo = ciService.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        } else {
            if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
                throw new CiAuthException();
            }
            ciVo.setNameAttrId(attrId);
            ciService.updateCiNameAttrId(ciVo);
        }
        return null;
    }

}
