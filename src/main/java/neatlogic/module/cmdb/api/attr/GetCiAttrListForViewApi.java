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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
        List<CiVo> upwardCiList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        for (CiVo ci : upwardCiList) {
            List<AttrVo> attrList = attrMapper.getDeclaredAttrListByCiId(ci.getId());
            for (AttrVo attr : attrList) {
                if (attrNameList.contains(attr.getName())) {
                    continue;
                }
                attrNameList.add(attr.getName());
                resultList.add(attr);
            }
        }

        return resultList;
    }
}
