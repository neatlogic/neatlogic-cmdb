/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.attr;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.exception.attr.AttrDeleteDeniedException;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CI_MODIFY;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.service.attr.AttrService;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrMapper attrMapper;


    @Override
    public String getToken() {
        return "/cmdb/attr/delete";
    }

    @Override
    public String getName() {
        return "删除模型属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Description(desc = "删除模型属性接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long attrId = jsonObj.getLong("id");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        if (attrVo.getIsPrivate() != null && attrVo.getIsPrivate().equals(1)) {
            throw new AttrDeleteDeniedException();
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(attrVo.getCiId()).check()) {
            throw new CiAuthException();
        }
        attrService.deleteAttr(attrVo);
        return null;
    }

}
