/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.citype;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiTypeCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/citype/search";
    }

    @Override
    public String getName() {
        return "获取模型类型和模型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "类型id列表"),
            @Param(name = "ciNameList", type = ApiParamType.JSONARRAY, desc = "模型名称列表"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, desc = "是否虚拟模型，0：否，1：是"),
            @Param(name = "isAbstract", type = ApiParamType.INTEGER, desc = "是否抽象模型，0：否，1：是")})
    @Output({@Param(explode = CiTypeVo[].class)})
    @Description(desc = "获取模型类型和模型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo pCiVo = JSONObject.toJavaObject(jsonObj, CiVo.class);
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
