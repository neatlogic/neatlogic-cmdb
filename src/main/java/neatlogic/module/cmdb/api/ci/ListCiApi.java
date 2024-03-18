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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/list";
    }

    @Override
    public String getName() {
        return "nmcac.listciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.ciidlist"),
            @Param(name = "excludeCiIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.excludeciid"),
            @Param(name = "isAbstract", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isabstractci"),
            @Param(name = "isVirtual", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isvirtualci"),
    })
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "nmcac.listciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        JSONArray excludeCiIdList = jsonObj.getJSONArray("excludeCiIdList");
        List<Long> ciIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                try {
                    ciIdList.add(idList.getLong(i));
                } catch (Exception ignored) {

                }
            }
        }
        List<CiVo> ciList = ciMapper.getAllCi(ciIdList);
        JSONArray jsonList = new JSONArray();
        Integer isAbstract = jsonObj.getInteger("isAbstract");
        Integer isVirtual = jsonObj.getInteger("isVirtual");
        CI:
        for (CiVo ciVo : ciList) {
            if (CollectionUtils.isNotEmpty(excludeCiIdList)) {
                for (int i = 0; i < excludeCiIdList.size(); i++) {
                    if (excludeCiIdList.getLong(i).equals(ciVo.getId())) {
                        continue CI;
                    }
                }
            }
            if (isAbstract != null && !Objects.equals(isAbstract, ciVo.getIsAbstract())) {
                continue;
            }
            if (isVirtual != null && !Objects.equals(isVirtual, ciVo.getIsVirtual())) {
                continue;
            }
            JSONObject valueObj = new JSONObject();
            valueObj.put("value", ciVo.getId());
            valueObj.put("text", ciVo.getLabel() + "(" + ciVo.getName() + ")");
            jsonList.add(valueObj);
        }
        return jsonList;
    }
}
