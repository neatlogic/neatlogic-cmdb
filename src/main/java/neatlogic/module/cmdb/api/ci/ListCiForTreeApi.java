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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiForTreeApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "nmcac.listcifortreeapi.getname";
    }

    @Override
    public String getToken() {
        return "cmdb/ci/listtree";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "isVirtual", type = ApiParamType.INTEGER, desc = "是否虚拟模型"),
            @Param(name = "rootCiId", type = ApiParamType.LONG, desc = "term.cmdb.rootciid"),
            @Param(name = "rootCiName", type = ApiParamType.STRING, desc = "term.cmdb.rootciname")
    })
    @Output({
            @Param(explode = ValueTextVo[].class)
    })
    @Description(desc = "nmcac.listcifortreeapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        Long rootCiId = jsonObj.getLong("rootCiId");
        Integer isVirtual = jsonObj.getInteger("isVirtual");
        List<CiVo> ciList = null;
        if (rootCiId == null) {
            String rootCiName = jsonObj.getString("rootCiName");
            if (StringUtils.isNotBlank(rootCiName)) {
                CiVo ciVo = ciMapper.getCiByName(rootCiName);
                if (ciVo == null) {
                    throw new CiNotFoundException(rootCiName);
                }
                rootCiId = ciVo.getId();
            }
        }
        if (rootCiId != null) {
            CiVo ciVo = ciMapper.getCiById(rootCiId);
            if (ciVo == null) {
                throw new CiNotFoundException(rootCiId);
            }
            ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        } else {
            ciList = ciMapper.getAllCi(null);
        }
        if (isVirtual != null) {
            ciList.removeIf(d -> !d.getIsVirtual().equals(isVirtual));
        }
        if (ciId != null) {
            ciList.removeIf(d -> d.getId().equals(ciId));
        }
        Map<Long, CiVo> ciMap = new HashMap<>();
        for (CiVo ciVo : ciList) {
            ciMap.put(ciVo.getId(), ciVo);
        }

        //将模型挂到父模型上
        for (CiVo ciVo : ciList) {
            if (ciVo.getParentCiId() != null) {
                CiVo parentCiVo = ciMap.get(ciVo.getParentCiId());
                if (parentCiVo != null) {
                    parentCiVo.addChild(ciVo);
                } else {
                    if (rootCiId != null && Objects.equals(rootCiId, ciVo.getId())) {
                        ciVo.setParentCiId(null);
                    }
                }
            }
        }
        //清除所有非父节点模型
        ciList.removeIf(ciVo -> ciVo.getParentCiId() != null || (ciId != null && checkCiIdIsParent(ciId, ciVo)));
        return ciList;
    }

    private boolean checkCiIdIsParent(Long ciId, CiVo ciVo) {
        while (ciVo.getParentCi() != null) {
            if (ciVo.getParentCi().getId().equals(ciId)) {
                return true;
            }
            ciVo = ciVo.getParentCi();
        }
        return false;
    }


}
