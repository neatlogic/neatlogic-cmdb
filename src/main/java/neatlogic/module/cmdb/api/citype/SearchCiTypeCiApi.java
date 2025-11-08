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

package neatlogic.module.cmdb.api.citype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiTypeCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/citype/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchcitypeciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "common.typeid"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcitypeciapi.input.param.desc.typeidlist"),
            @Param(name = "ciNameList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcitypeciapi.input.param.desc.cinamelist"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, desc = "nmcac.searchcitypeciapi.input.param.desc.isvirtual"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, desc = "nmcac.searchcitypeciapi.input.param.desc.isabstract")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "nmcac.searchcitypeciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo pCiVo = JSON.toJavaObject(jsonObj, CiVo.class);
        //查询ciNameList的ciTypeIdList
        JSONArray ciNameList = jsonObj.getJSONArray("ciNameList");
        if (CollectionUtils.isNotEmpty(ciNameList)) {
            List<CiVo> ciListByName = ciMapper.getCiListByNameList(ciNameList.toJavaList(String.class));
            if (CollectionUtils.isNotEmpty(ciListByName)) {
                List<Long> typeIdList = pCiVo.getTypeIdList();
                if (CollectionUtils.isNotEmpty(typeIdList)) {
                    pCiVo.getTypeIdList().addAll(ciListByName.stream().map(CiVo::getTypeId).collect(Collectors.toList()));
                } else {
                    pCiVo.setTypeIdList(ciListByName.stream().map(CiVo::getTypeId).collect(Collectors.toList()));
                }
            }
        }
        List<CiTypeVo> ciTypeList = ciMapper.searchCiTypeCi(pCiVo);
        //如果没有管理权限则需要检查每个模型的权限
        if (!AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY")) {
            for (CiTypeVo ciType : ciTypeList) {
                Iterator<CiVo> itCi = ciType.getCiList().iterator();
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
        }
        return ciTypeList;
    }
}
