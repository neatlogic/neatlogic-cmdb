/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.attr;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.exception.attr.AttrNameRepeatException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.attr.AttrService;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAttrApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;


    @Resource
    private AttrService attrService;

    @Override
    public String getToken() {
        return "/cmdb/attr/save";
    }

    @Override
    public String getName() {
        return "nmcaa.saveattrapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "term.cmdb.addid"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "type", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.attrtype"),
            @Param(name = "targetCiId", type = ApiParamType.LONG, desc = "term.cmdb.targetciid"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "term.cmdb.attrconfig"),
            //name不能大于25个字符，因为mysql表名最长64字符，需要给模型名留下位置
            @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 25, desc = "common.uniquename"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "common.cnname", xss = true, maxLength = 100,
                    isRequired = true),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.memo", maxLength = 500, xss = true),
            @Param(name = "validator", type = ApiParamType.STRING, desc = "term.cmdb.validatehandler"),
            @Param(name = "validConfig", type = ApiParamType.JSONOBJECT, desc = "term.cmdb.validateconfig"),
            @Param(name = "isRequired", type = ApiParamType.INTEGER, desc = "common.isrequired"),
            @Param(name = "isUnique", type = ApiParamType.INTEGER, desc = "common.isunique"),
            @Param(name = "isSearchAble", type = ApiParamType.INTEGER, desc = "term.cmdb.issearchable"),
            @Param(name = "inputType", type = ApiParamType.ENUM, rule = "at,mt", desc = "nmcaa.saveattrapi.input.param.desc.inputtype"),
            @Param(name = "groupName", type = ApiParamType.STRING, desc = "common.group")})
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "nmcaa.getattrapi.input.param.desc.id"),})
    @Description(desc = "nmcaa.saveattrapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AttrVo attrVo = JSONObject.toJavaObject(jsonObj, AttrVo.class);
        Long attrId = jsonObj.getLong("id");
        if (!CiAuthChecker.chain().checkCiManagePrivilege(attrVo.getCiId()).check()) {
            throw new CiAuthException();
        }
        //校验name是否重复
        if (attrMapper.checkAttrNameIsRepeat(attrVo) > 0) {
            throw new AttrNameRepeatException(attrVo.getName());
        }

        if (attrId == null) {
            attrService.insertAttr(attrVo);
        } else {
            attrService.updateAttr(attrVo);
        }
        return attrVo.getId();
    }

}
