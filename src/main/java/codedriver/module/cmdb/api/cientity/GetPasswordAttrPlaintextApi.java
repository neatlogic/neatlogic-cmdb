/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiAuthException;
import codedriver.framework.cmdb.exception.cientity.AttrEntityNotFoundException;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPasswordAttrPlaintextApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getName() {
        return "获取密码属性明文";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "属性id")})
    @Output({@Param(type = ApiParamType.STRING, desc = "密码明文")})
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long attrId = jsonObj.getLong("attrId");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }
        CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(attrVo.getCiId(), ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        if (!CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).checkCiEntityIsInGroup(ciEntityId, GroupType.MAINTAIN, GroupType.READONLY).check()) {
            throw new CiAuthException();
        }

        AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
        if (attrEntityVo == null) {
            throw new AttrEntityNotFoundException(attrId);
        }
        if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
            IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrEntityVo.getAttrType());
            handler.transferValueListToDisplay(attrVo, attrEntityVo.getValueList());
            return attrEntityVo.getValueList().getString(0);
        }
        return "";
    }


    @Override
    public String getToken() {
        return "/cmdb/attrentity/getplaintext";
    }
}
