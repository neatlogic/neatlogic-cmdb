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

package neatlogic.module.cmdb.api.rel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.cmdb.exception.rel.RelNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.rel.RelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteRelApi extends PrivateApiComponentBase {

    @Resource
    private RelService relService;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/rel/delete";
    }

    @Override
    public String getName() {
        return "删除模型关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "关系id")})
    @Description(desc = "删除模型关系")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long relId = jsonObj.getLong("id");
        RelVo relVo = relMapper.getRelById(relId);
        if (relVo == null) {
            throw new RelNotFoundException(relId);
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getFromCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getFromCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        if (!CiAuthChecker.chain().checkCiManagePrivilege(relVo.getToCiId()).check()) {
            CiVo ciVo = ciMapper.getCiById(relVo.getToCiId());
            throw new CiAuthException(ciVo.getLabel());
        }
        relService.deleteRel(relVo);
        return null;
    }

}
