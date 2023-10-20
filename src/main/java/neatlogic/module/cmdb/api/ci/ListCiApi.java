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
            @Param(name = "isAbstract", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isabstractci"),
            @Param(name = "isVirtual", type = ApiParamType.ENUM, rule = "0,1", desc = "term.cmdb.isvirtualci"),
    })
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "nmcac.listciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
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
        for (CiVo ciVo : ciList) {
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
