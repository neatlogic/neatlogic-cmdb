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
