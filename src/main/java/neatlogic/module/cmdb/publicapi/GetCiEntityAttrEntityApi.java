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

package neatlogic.module.cmdb.publicapi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.utils.CiEntityBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityAttrEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/attrentity/get";
    }

    @Override
    public String getName() {
        return "获取配置项属性信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "attrList", type = ApiParamType.JSONARRAY, desc = "属性名称列表"),
    })
    @Output({@Param(explode = CiEntityVo.class)})
    @Description(desc = "获取配置项属性信息接口，自动化巡检时使用")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        JSONArray attrList = jsonObj.getJSONArray("attrList");
        CiEntityVo ciEntityBaseVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
        if (ciEntityBaseVo == null) {
            throw new CiEntityNotFoundException(ciEntityId);
        }
        CiEntityVo ciEntityVo = getCiEntityByIdLite(ciEntityBaseVo.getCiId(), ciEntityId, attrList);

        JSONObject entityObj = new JSONObject();
        entityObj.put("id", ciEntityVo.getId());
        entityObj.put("uuid", ciEntityVo.getUuid());
        entityObj.put("name", ciEntityVo.getName());
        entityObj.put("ciId", ciEntityVo.getCiId());
        entityObj.put("type", ciEntityVo.getTypeId());
        entityObj.put("typeName", ciEntityVo.getTypeName());
        entityObj.put("attrEntityData", ciEntityVo.getAttrEntityData());
        return entityObj;
    }

    private CiEntityVo getCiEntityByIdLite(Long ciId, Long ciEntityId, JSONArray attrNameList) {
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        CiEntityVo ciEntityVo = new CiEntityVo();
        List<CiVo> ciList;
        if (ciVo.getIsVirtual().equals(0)) {
            ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        } else {
            ciList = new ArrayList<>();
            ciList.add(ciVo);
        }
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
        attrList = attrList.stream().filter(d -> {
            for (int i = 0; i < attrNameList.size(); i++) {
                String attrName = attrNameList.getString(i);
                if (attrName.equalsIgnoreCase(d.getName())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        ciEntityVo.setCiList(ciList);
        ciEntityVo.setId(ciEntityId);
        ciEntityVo.setCiId(ciVo.getId());
        ciEntityVo.setCiLabel(ciVo.getLabel());
        ciEntityVo.setCiName(ciVo.getName());
        ciEntityVo.setAttrList(attrList);

        List<HashMap<String, Object>> resultList = ciEntityMapper.getCiEntityByIdLite(ciEntityVo);
        CiEntityVo returnCiEntityVo = new CiEntityBuilder.Builder(ciEntityVo, resultList, ciVo, attrList, null).build().getCiEntity();
        if (returnCiEntityVo != null) {
            //拼接引用属性数据
            Long attrEntityLimit = null;
            if (CollectionUtils.isNotEmpty(attrList)) {
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getTargetCiId() != null) {
                        List<AttrEntityVo> attrEntityList = ciEntityMapper.getAttrEntityByAttrIdAndFromCiEntityId(returnCiEntityVo.getId(), attrVo.getId(), attrEntityLimit);
                        if (CollectionUtils.isNotEmpty(attrEntityList)) {
                            JSONArray valueList = new JSONArray();
                            for (AttrEntityVo attrEntityVo : attrEntityList) {
                                valueList.add(attrEntityVo.getToCiEntityId());
                            }
                            JSONArray actualValueList = new JSONArray();
                            if (CollectionUtils.isNotEmpty(valueList)) {
                                actualValueList = AttrValueHandlerFactory.getHandler(attrVo.getType()).getActualValueList(attrVo, valueList);
                            }
                            returnCiEntityVo.addAttrEntityData(attrVo.getId(), CiEntityBuilder.buildAttrObj(returnCiEntityVo.getId(), attrVo, valueList, actualValueList));
                        }
                    }
                }
            }
        }
        return returnCiEntityVo;
    }

}
