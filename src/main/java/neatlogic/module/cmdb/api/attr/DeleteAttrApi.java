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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.exception.attr.AttrDeleteDeniedException;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.attr.AttrService;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAttrApi extends PrivateApiComponentBase {

    @Resource
    private AttrService attrService;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/delete";
    }

    @Override
    public String getName() {
        return "nmcaa.deleteattrapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Description(desc = "nmcaa.deleteattrapi.getname")
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
