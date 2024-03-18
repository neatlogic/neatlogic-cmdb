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
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "common.displaytype"),
            @Param(name = "needAlias", type = ApiParamType.INTEGER, desc = "term.cmdb.needalias", rule = "0,1")})
    @Output({@Param(explode = GlobalAttrEntityVo[].class)})
    @Description(desc = "nmcag.getcientityglobalattrentityapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciEntityId = paramObj.getLong("ciEntityId");
        String showType = paramObj.getString("showType");
        int needAlias = paramObj.getIntValue("needAlias");
        CiEntityVo ciEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
        if (ciEntityVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        List<GlobalAttrEntityVo> returnList = new ArrayList<>();
        /*
        先搜索出所有全局属性再和配置了全局属性的数据合并，这样做的目的是全局属性随时可能会增加，为了能正确显示新添加的全局属性，需要两份数据做合并
         */
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(ciEntityVo.getCiId());
        List<CiViewVo> ciViewList;
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciEntityVo.getCiId());
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("global")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            globalAttrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        } else {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciEntityVo.getCiId());
            ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        }
        if (needAlias == 1 && CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciView : ciViewList) {
                if (StringUtils.isNotBlank(ciView.getAlias()) && ciView.getType().equals("global")) {
                    Optional<GlobalAttrVo> op = globalAttrList.stream().filter(d -> d.getId().equals(ciView.getItemId())).findFirst();
                    op.ifPresent(attrVo -> attrVo.setLabel(ciView.getAlias()));
                }
            }
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
