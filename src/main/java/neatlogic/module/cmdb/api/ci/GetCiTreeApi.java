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

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/tree";
    }

    @Override
    public String getName() {
        return "nmcac.getciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "nmcac.getciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<CiVo> ciList = ciMapper.getCiTree();
        //如果没有管理权限则需要检查每个模型的权限
        if (!AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY")) {
            Iterator<CiVo> itCi = ciList.iterator();
            while (itCi.hasNext()) {
                CiVo ciVo = itCi.next();
                if (CollectionUtils.isNotEmpty(ciVo.getAuthList())) {
                    if (!CiAuthChecker.hasPrivilege(ciVo.getAuthList(), CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY)) {
                        if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                            itCi.remove();
                        }
                    }
                } else {
                    if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                        itCi.remove();
                    }
                }
            }
        }
        JSONArray returnList = new JSONArray();
        for (CiVo ciVo : ciList) {
            JSONObject ciObj = new JSONObject();
            ciObj.put("id", ciVo.getId());
            ciObj.put("parentCiId", ciVo.getParentCiId(true));
            ciObj.put("icon", ciVo.getIcon());
            ciObj.put("name", ciVo.getName());
            ciObj.put("label", ciVo.getLabel());
            ciObj.put("sort", ciVo.getSort());
            returnList.add(ciObj);
        }
        return returnList;
    }


}
