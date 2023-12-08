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
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.GlobalAttrEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityGlobalAttrEntityApi extends PrivateApiComponentBase {

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getName() {
        return "nmcag.getcientityglobalattrentityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityId", isRequired = true, type = ApiParamType.LONG, desc = "term.cmdb.cientityid"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "common.displaytype"),})
    @Output({@Param(explode = GlobalAttrEntityVo[].class)})
    @Description(desc = "nmcag.getcientityglobalattrentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciEntityId = paramObj.getLong("ciEntityId");
        String showType = paramObj.getString("showType");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        List<GlobalAttrEntityVo> returnList = new ArrayList<>();
        /*
        先搜索出所有全局属性再和配置了全局属性的数据合并，这样做的目的是全局属性随时可能会增加，为了能正确显示新添加的全局属性，需要两份数据做合并
         */
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(ciEntityVo.getCiId());

        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciEntityVo.getCiId());
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("global")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            globalAttrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        }

        if (CollectionUtils.isNotEmpty(globalAttrList)) {
            List<GlobalAttrEntityVo> attrEntityList = globalAttrMapper.getGlobalAttrByCiEntityId(ciEntityId);
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
