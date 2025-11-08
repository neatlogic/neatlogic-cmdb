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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.TransactionActionType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateRelEntityValidDayApi extends PrivateApiComponentBase {

    @Resource
    private RelEntityMapper relEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/relentity/updatevalidday";
    }

    @Override
    public String getName() {
        return "nmcac.updaterelentityvaliddayapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "nmcac.searchcientityapi.input.param.desc.relid"),
            @Param(name = "validDay", type = ApiParamType.INTEGER, isRequired = true, desc = "nmcac.updaterelentityvaliddayapi.input.param.desc"),
    })
    @Description(desc = "nmcac.updaterelentityvaliddayapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        Integer validDay = jsonObj.getInteger("validDay");
        RelEntityVo relEntityVo = relEntityMapper.getRelEntityById(id);
        relEntityVo.setValidDay(Math.max(0, validDay));
        //关系两端随便一个有权限即可进行编辑
        if (!CiAuthChecker.chain().checkCiEntityUpdatePrivilege(relEntityVo.getFromCiId()).checkCiEntityIsInGroup(relEntityVo.getFromCiEntityId(), GroupType.MAINTAIN).check()
                && !CiAuthChecker.chain().checkCiEntityUpdatePrivilege(relEntityVo.getToCiId()).checkCiEntityIsInGroup(relEntityVo.getToCiEntityId(), GroupType.MAINTAIN).check()) {
            throw new CiEntityAuthException(TransactionActionType.UPDATE.getText());
        }
        relEntityMapper.updateRelEntityValidDay(relEntityVo);
        return null;
    }

}
