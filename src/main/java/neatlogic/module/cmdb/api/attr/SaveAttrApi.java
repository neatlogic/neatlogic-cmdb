/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.attr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAttrApi extends PrivateApiComponentBase {

    @Resource
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
        AttrVo attrVo = JSON.toJavaObject(jsonObj, AttrVo.class);
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
