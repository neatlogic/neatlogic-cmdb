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
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiAttrRelListApi extends PrivateApiComponentBase {


    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/attrrel/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchciattrrellistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmcac.searchciattrrellistapi.input.param.desc.keyword")})
    @Output({@Param(type = ApiParamType.JSONARRAY)})
    @Description(desc = "nmcac.searchciattrrellistapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String keyword = jsonObj.getString("keyword");
        JSONArray returnList = new JSONArray();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        List<GlobalAttrVo> globalAttrList = globalAttrMapper.getGlobalAttrByCiId(ciId);
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        ciViewVo.setNeedAlias(1);
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase(Locale.ROOT);
            //因为有二级缓存，数据量也不大，可以直接在应用层过滤关键字实现搜索效果
            String finalKeyword = keyword;
            attrList = attrList.stream().filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(finalKeyword)).collect(Collectors.toList());
            relList = relList.stream().filter(d -> (d.getDirection().equals(RelDirectionType.FROM.getValue()) && d.getToName().toLowerCase(Locale.ROOT).contains(finalKeyword)) || (d.getDirection().equals(RelDirectionType.TO.getValue()) && d.getFromName().toLowerCase(Locale.ROOT).contains(finalKeyword))).collect(Collectors.toList());
            globalAttrList = globalAttrList.stream().filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(finalKeyword)).collect(Collectors.toList());
        }
        for (GlobalAttrVo globalAttrVo : globalAttrList) {
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("global") && d.getItemId().equals(globalAttrVo.getId())).findAny();
                op.ifPresent(viewVo -> globalAttrVo.setLabel(viewVo.getAlias()));
            }
            JSONObject attrObj = new JSONObject();
            attrObj.put("id", globalAttrVo.getId());
            attrObj.put("uid", "global_" + globalAttrVo.getId());
            attrObj.put("name", globalAttrVo.getName());
            attrObj.put("label", globalAttrVo.getLabel());
            returnList.add(attrObj);
        }
        for (AttrVo attrVo : attrList) {
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("attr") && d.getItemId().equals(attrVo.getId())).findAny();
                op.ifPresent(viewVo -> attrVo.setLabel(viewVo.getAlias()));
            }
            JSONObject attrObj = new JSONObject();
            attrObj.put("id", attrVo.getId());
            attrObj.put("uid", "attr_" + attrVo.getId());
            attrObj.put("name", attrVo.getName());
            attrObj.put("label", attrVo.getLabel());
            attrObj.put("targetCiId", attrVo.getTargetCiId());
            returnList.add(attrObj);
        }
        for (RelVo relVo : relList) {
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().startsWith("rel") && d.getItemId().equals(relVo.getId())).findAny();
                if (op.isPresent()) {
                    CiViewVo ciview = op.get();
                    if (ciview.getType().equals("relfrom")) {
                        relVo.setToLabel(ciview.getAlias());
                    } else if (ciview.getType().equals("relto")) {
                        relVo.setFromLabel(ciview.getAlias());
                    }
                }
            }
            JSONObject relObj = new JSONObject();
            relObj.put("id", relVo.getId());
            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                relObj.put("uid", "relfrom_" + relVo.getId());
                relObj.put("name", relVo.getToName());
                relObj.put("label", relVo.getToLabel());
                relObj.put("targetCiId", relVo.getToCiId());
            } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                relObj.put("uid", "relto_" + relVo.getId());
                relObj.put("name", relVo.getFromName());
                relObj.put("label", relVo.getFromLabel());
                relObj.put("targetCiId", relVo.getFromCiId());
            }
            returnList.add(relObj);
        }


        return returnList;
    }
}
