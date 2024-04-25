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
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiAttrListForViewApi extends PrivateApiComponentBase {

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/listattr/forview";
    }

    @Override
    public String getName() {
        return "nmcaa.getciattrlistforviewapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "term.cmdb.ciname")
    })
    @Output({
            @Param(name = "Return", explode = AttrVo[].class)
    })
    @Description(desc = "nmcaa.getciattrlistforviewapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ciVo = null;
        Long ciId = jsonObj.getLong("ciId");
        if (ciId != null) {
            ciVo = ciMapper.getCiById(ciId);
            if (ciVo == null) {
                throw new CiNotFoundException(ciId);
            }
        } else {
            String ciName = jsonObj.getString("ciName");
            if (StringUtils.isBlank(ciName)) {
                throw new ParamNotExistsException("ciId", "ciName");
            }
            ciVo = ciMapper.getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
        }
        List<AttrVo> resultList = new ArrayList<>();
        List<String> attrNameList = new ArrayList<>();
        List<Long> targetCiIdList = new ArrayList<>();
        List<CiVo> upwardCiList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        for (CiVo ci : upwardCiList) {
            List<AttrVo> attrList = attrMapper.getDeclaredAttrListByCiId(ci.getId());
            for (AttrVo attr : attrList) {
                if (attrNameList.contains(attr.getName())) {
                    continue;
                }
                attrNameList.add(attr.getName());
                if (attr.getTargetCiId() != null) {
                    targetCiIdList.add(attr.getTargetCiId());
                }
                resultList.add(attr);
            }
        }
        if (CollectionUtils.isNotEmpty(targetCiIdList)) {
            List<CiVo> targetCiList = ciMapper.getAllCi(targetCiIdList);
            Map<Long, CiVo> targetCiMap = targetCiList.stream().collect(Collectors.toMap(CiVo::getId, e -> e));
            for (AttrVo attr : resultList) {
                if (attr.getTargetCiId() == null) {
                    continue;
                }
                CiVo targetCi = targetCiMap.get(attr.getTargetCiId());
                if (targetCi != null) {
                    attr.setTargetCiName(targetCi.getName());
                }
            }
        }
        return resultList;
    }
}
