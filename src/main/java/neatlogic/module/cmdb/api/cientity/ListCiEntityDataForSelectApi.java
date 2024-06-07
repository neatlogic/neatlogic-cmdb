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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelFilterVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.matrix.constvalue.SearchExpression;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2024/05/23 10:50
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiEntityDataForSelectApi extends PrivateApiComponentBase {
    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "cmdb/cientity/data/list/forselect";
    }

    @Override
    public String getName() {
        return "nmcac.listcientitydataforselectapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "nmcac.listcientitydataforselectapi.input.param.desc.elementid"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "ocommn.isneedpage")
    })
    @Output({
            @Param(explode = ValueTextVo[].class)
    })
    @Description(desc = "nmcac.listcientitydataforselectapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tbodyList = new JSONArray();
        Long ciId = paramObj.getLong("ciId");
        String label = paramObj.getString("label");
        List<Map<String, JSONObject>> resultList = new ArrayList<>();
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(ciId);
        ciEntityVo.setCurrentPage(paramObj.getInteger("currentPage"));
        ciEntityVo.setPageSize(paramObj.getInteger("pageSize"));
        Map<Long, AttrVo> attrMap = new HashMap<>();
        Map<Long, RelVo> relMap = new HashMap<>();
        Map<String, CiViewVo> ciViewMap = new HashMap<>();
        ciEntityService.getCiViewMapAndAttrMapAndRelMap(ciId, attrMap, relMap, ciViewMap);
        CiViewVo ciView = ciViewMap.get(label);
        if (ciView != null) {
            List<AttrFilterVo> attrFilters = new ArrayList<>();
            List<RelFilterVo> relFilters = new ArrayList<>();
            List<Long> attrIdList = new ArrayList<>();
            List<Long> relIdList = new ArrayList<>();
            List<String> valueList = null;
            String keyword = paramObj.getString("keyword");
            if(StringUtils.isNotBlank(keyword)) {
                valueList = Collections.singletonList(keyword);
            }
            JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
            if(CollectionUtils.isNotEmpty(defaultValue)) {
                valueList = defaultValue.toJavaList(String.class);
            }
            String expression = SearchExpression.LI.getExpression();
            if(CollectionUtils.isNotEmpty(defaultValue)) {
                expression = SearchExpression.EQ.getExpression();
            }
            boolean flag = true;
            switch (ciView.getType()) {
                case "attr":
                    Long attrId = Long.valueOf(label.substring(5));
                    attrIdList.add(attrId);
                    AttrVo attrVo = attrMap.get(attrId);
                    if (attrVo != null) {
                        AttrFilterVo attrFilterVo = ciEntityService.convertAttrFilter(attrVo, expression, valueList);
                        if (attrFilterVo != null) {
                            attrFilters.add(attrFilterVo);
                        } else {
                            flag = false;
                        }
                    }
                    break;
                case "relfrom":
                    long relId = Long.parseLong(label.substring(8));
                    relIdList.add(relId);
                    RelVo relVo = relMap.get(relId);
                    if (relVo != null) {
                        RelFilterVo relFilterVo = ciEntityService.convertFromRelFilter(relVo, expression, valueList, "from");
                        if (relFilterVo != null) {
                            relFilters.add(relFilterVo);
                        } else {
                            flag = false;
                        }
                    }
                    break;
                case "relto":
                    relId = Long.parseLong(label.substring(6));
                    relIdList.add(relId);
                    relVo = relMap.get(relId);
                    if (relVo != null) {
                        RelFilterVo relFilterVo = ciEntityService.convertFromRelFilter(relVo, expression, valueList, "to");
                        if (relFilterVo != null) {
                            relFilters.add(relFilterVo);
                        } else {
                            flag = false;
                        }
                    }
                    break;
                case "const":
                    //固化属性需要特殊处理
                    if ("const_id".equals(label)) {
                        List<Long> idList = new ArrayList<>();
                        for (String value : valueList) {
                            idList.add(Long.valueOf(value));
                        }
                        ciEntityVo.setIdList(idList);
                    } else if ("const_ciLabel".equals(label)) {
                        List<CiVo> ciList = ciMapper.getCiListByLabelList(valueList);
                        List<Long> filterCiIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                        ciEntityVo.setFilterCiIdList(filterCiIdList);
                    }
                    break;
                default:
                    break;
            }
            if (!flag) {
                return resultList;
            }
            ciEntityVo.setAttrFilterList(attrFilters);
            ciEntityVo.setRelFilterList(relFilters);
            ciEntityVo.setAttrIdList(attrIdList);
            ciEntityVo.setRelIdList(relIdList);
            List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                ciEntityList.forEach(ciEntity -> {
                    JSONObject valueJson = ciEntityService.getTbodyRowData(ciEntity);
                    if (MapUtils.isNotEmpty(valueJson)) {
                        String value = valueJson.getString(label);
                        tbodyList.add(new ValueTextVo(value, value));
                    }
                });
            }
        }
        return TableResultUtil.getResult(tbodyList, ciEntityVo);
    }
}
