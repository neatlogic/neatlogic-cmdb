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

package neatlogic.module.cmdb.api.globalattr;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cientity.GlobalAttrEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityGlobalAttrEntityApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Override
    public String getName() {
        return "nmcag.getcientityglobalattrentityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "配置项id")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = GlobalAttrVo[].class)})
    @Description(desc = "nmcag.getcientityglobalattrentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<GlobalAttrEntityVo> returnList = new ArrayList<>();
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(new GlobalAttrVo() {{
            this.setIsActive(1);
        }});
        if (CollectionUtils.isNotEmpty(globalAttrList)) {
            List<GlobalAttrEntityVo> attrEntityList = globalAttrMapper.getGlobalAttrByCiEntityId(paramObj.getLong("ciEntityId"));
            for (GlobalAttrVo globalAttrVo : globalAttrList) {
                GlobalAttrEntityVo entity = new GlobalAttrEntityVo();
                entity.setAttrId(globalAttrVo.getId());
                entity.setAttrLabel(globalAttrVo.getLabel());
                entity.setAttrName(globalAttrVo.getName());
                Optional<GlobalAttrEntityVo> op = attrEntityList.stream().filter(d -> d.getAttrId().equals(globalAttrVo.getId())).findFirst();
                op.ifPresent(globalAttrEntityVo -> entity.setValueList(globalAttrEntityVo.getValueList()));
                returnList.add(entity);
            }

        }
        return returnList;
    }

    @Override
    public String getToken() {
        return "/cmdb/cientity/globalattr/get";
    }


}
