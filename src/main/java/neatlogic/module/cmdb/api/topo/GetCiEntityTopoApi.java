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

package neatlogic.module.cmdb.api.topo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTopoTemplateVo;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityTopoVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.exception.ci.CiTopoTemplateNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.graphviz.Graphviz;
import neatlogic.framework.graphviz.Layer;
import neatlogic.framework.graphviz.Link;
import neatlogic.framework.graphviz.Node;
import neatlogic.framework.graphviz.enums.LayoutType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTopoTemplateMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
@Transactional
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTopoApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(GetCiEntityTopoApi.class);

    @Resource
    private CiTopoTemplateMapper ciTopoTemplateMapper;


    @Resource
    private CiTypeMapper ciTypeMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getToken() {
        return "/cmdb/topo/cientity";
    }

    @Override
    public String getName() {
        return "nmcat.getcientitytopoapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isBackbone", type = ApiParamType.INTEGER, rule = "0,1", isRequired = true, desc = "term.cmdb.isbackbone"),
            @Param(name = "layout", type = ApiParamType.ENUM, rule = "dot,circo,fdp,neato,osage,patchwork,twopi", isRequired = true, desc = "common.layout"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "globalAttrFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.globalattrfilterlist"),
            @Param(name = "disableRelList", type = ApiParamType.JSONARRAY, desc = "nmcat.getcientitytopoapi.input.param.desc.disablerellist"),
            @Param(name = "templateId", type = ApiParamType.LONG, desc = "term.autoexec.scenarioid"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "nmcat.getcientitytopoapi.input.param.desc.level"),
            @Param(name = "templateConfig", type = ApiParamType.JSONOBJECT, desc = "term.cmdb.templateconfig")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "nmcat.getcientitytopoapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String layout = jsonObj.getString("layout");
        int isBackbone = jsonObj.getIntValue("isBackbone");
        Long templateId = jsonObj.getLong("templateId");
        JSONObject templateConfig = jsonObj.getJSONObject("templateConfig");
        // 所有需要绘制的配置项
        Set<CiEntityVo> ciEntitySet = new HashSet<>();
        // 所有需要绘制的层次
        Set<Long> ciTypeIdSet = new HashSet<>();
        // 所有需要绘制的关系
        Set<RelEntityVo> relEntitySet = new HashSet<>();
        //记录已经绘制的配置项
        Set<String> ciEntityNodeSet = new HashSet<>();

        Map<Long, RelTypeVo> relTypeMap = new HashMap<>();


        Long ciEntityId = jsonObj.getLong("ciEntityId");
        JSONArray disableRelObjList = jsonObj.getJSONArray("disableRelList");
        JSONArray globalAttrFilterObjList = jsonObj.getJSONArray("globalAttrFilterList");
        List<GlobalAttrFilterVo> globalAttrFilterList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(globalAttrFilterObjList)) {
            for (int i = 0; i < globalAttrFilterObjList.size(); i++) {
                GlobalAttrFilterVo globalAttrFilterVo = JSON.toJavaObject(globalAttrFilterObjList.getJSONObject(i), GlobalAttrFilterVo.class);
                globalAttrFilterList.add(globalAttrFilterVo);
            }
        }
        Set<Long> disableRelIdList = new HashSet<>();
        //已经使用过的关系，如果是backbone模式，查询过的关系不会重复查询
        Set<Long> containRelIdSet = new HashSet<>();
        JSONObject returnObj = new JSONObject();
        if (CollectionUtils.isNotEmpty(disableRelObjList)) {
            for (int i = 0; i < disableRelObjList.size(); i++) {
                disableRelIdList.add(disableRelObjList.getLong(i));
            }
        }
        //分组属性
       /* List<String> clusterAttrList = new ArrayList<>();
        clusterAttrList.add("tomcat#port");
        clusterAttrList.add("hardware#brand");
        clusterAttrList.add("system#env");
        Map<String, Cluster.Builder> clusterMap = new HashMap<>();*/
        // 先搜索出所有层次，因为需要按照层次顺序展示
        CiTypeVo pCiTypeVo = new CiTypeVo();
        pCiTypeVo.setIsShowInTopo(1);
        List<CiTypeVo> ciTypeList = ciTypeMapper.searchCiType(pCiTypeVo);
        Set<Long> canShowCiTypeIdSet = new HashSet<>();
        for (CiTypeVo ciTypeVo : ciTypeList) {
            canShowCiTypeIdSet.add(ciTypeVo.getId());
        }
        Integer level = jsonObj.getInteger("level");
        if (level == null) {
            level = 1;
        }

        List<Long> levelCiEntityIdList = new ArrayList<>();
        List<Long> excludeRelIdList = new ArrayList<>();
        levelCiEntityIdList.add(ciEntityId);
        if (templateId == null) {
            //获取起始节点
            CiEntityVo rootCiEntityVo = ciEntityMapper.getCiEntityBaseInfoById(ciEntityId);
            ciEntitySet.add(rootCiEntityVo);
            ciTypeIdSet.add(rootCiEntityVo.getTypeId());
            for (int l = 0; l <= level; l++) {
                if (CollectionUtils.isNotEmpty(levelCiEntityIdList)) {
                    List<Long> tmpCiCiEntityIdList = new ArrayList<>();
                    List<Long> tmpExcludeRelIdList = new ArrayList<>();
                    List<RelEntityVo> relEntityList = ciEntityMapper.getRelEntityByCiEntityIdList(levelCiEntityIdList, isBackbone == 1 ? excludeRelIdList : null);
                    if (CollectionUtils.isNotEmpty(relEntityList)) {
                        for (RelEntityVo relEntityVo : relEntityList) {
                            CiEntityVo ciEntityVo = new CiEntityVo();
                            if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                ciEntityVo.setTypeId(relEntityVo.getToCiTypeId());
                                ciEntityVo.setId(relEntityVo.getToCiEntityId());
                                ciEntityVo.setName(relEntityVo.getToCiEntityName());
                                ciEntityVo.setCiId(relEntityVo.getToCiId());
                                ciEntityVo.setCiIcon(relEntityVo.getToCiIcon());
                            } else {
                                ciEntityVo.setTypeId(relEntityVo.getFromCiTypeId());
                                ciEntityVo.setId(relEntityVo.getFromCiEntityId());
                                ciEntityVo.setName(relEntityVo.getFromCiEntityName());
                                ciEntityVo.setCiId(relEntityVo.getFromCiId());
                                ciEntityVo.setCiIcon(relEntityVo.getFromCiIcon());
                            }
                            if (canShowCiTypeIdSet.contains(ciEntityVo.getTypeId())) {
                                ciEntitySet.add(ciEntityVo);
                                ciTypeIdSet.add(ciEntityVo.getTypeId());
                            }
                            containRelIdSet.add(relEntityVo.getRelId());
                            RelTypeVo relTypeVo = relTypeMap.get(relEntityVo.getRelId());
                            if (relTypeVo == null) {
                                relTypeMap.put(relEntityVo.getRelId(), relMapper.getRelTypeByRelId(relEntityVo.getRelId()));
                                relTypeVo = relTypeMap.get(relEntityVo.getRelId());
                            }
                            //判断关系类型是否展示TOPO
                            if (relTypeVo != null && relTypeVo.getIsShowInTopo().equals(1)) {
                                if (CollectionUtils.isEmpty(disableRelIdList) || disableRelIdList.stream().noneMatch(r -> r.equals(relEntityVo.getRelId()))) {
                                    relEntitySet.add(relEntityVo);
                                    // 检查关系中的对端配置项是否已经存在，不存在可进入下一次搜索
                                    if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                        tmpCiCiEntityIdList.add(relEntityVo.getToCiEntityId());
                                        tmpExcludeRelIdList.add(relEntityVo.getRelId());
                                    } else if (relEntityVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                                        tmpCiCiEntityIdList.add(relEntityVo.getFromCiEntityId());
                                        tmpExcludeRelIdList.add(relEntityVo.getRelId());
                                    }
                                }
                            }
                        }
                    } else {
                        break;
                    }
                    levelCiEntityIdList = tmpCiCiEntityIdList;
                    excludeRelIdList = tmpExcludeRelIdList;

                } else {
                    break;
                }
            }
        } else {
            CiTopoTemplateVo ciTopoTemplateVo = ciTopoTemplateMapper.getCiTopoTemplateById(templateId);
            if (ciTopoTemplateVo == null) {
                throw new CiTopoTemplateNotFoundException(templateId);
            }
            if (MapUtils.isNotEmpty(ciTopoTemplateVo.getConfig()) && ciTopoTemplateVo.getConfig().get("ciRelList") != null) {
                JSONArray ciRelList = ciTopoTemplateVo.getConfig().getJSONArray("ciRelList");
                int startIndex = 0;
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setGlobalAttrFilterList(globalAttrFilterList);
                ciEntityVo.setIdList(new ArrayList<Long>() {{
                    this.add(ciEntityId);
                }});
                if (MapUtils.isNotEmpty(templateConfig)) {
                    for (int i = 0; i < ciRelList.size(); i++) {
                        JSONObject relObj = ciRelList.getJSONObject(i);
                        String relId = relObj.getString("relId");
                        if (templateConfig.containsKey(relId)) {
                            JSONObject configObj = templateConfig.getJSONObject(relId);
                            if (relObj.getIntValue("isHidden") == 0 && configObj.getBooleanValue("isHidden")) {
                                relObj.put("isHidden", 1);
                            } else if (relObj.getIntValue("isHidden") == 1 && configObj.getBooleanValue("isShow")) {
                                relObj.put("isHidden", 0);
                            }
                        }
                    }
                }
                while (startIndex < ciRelList.size()) {
                    List<RelVo> relList = new ArrayList<>();
                    for (int i = startIndex; i < ciRelList.size(); i++) {
                        JSONObject ciRelObj = ciRelList.getJSONObject(i);
                        RelVo relVo = new RelVo();
                        relVo.setId(ciRelObj.getLong("relId"));
                        relVo.setDirection(ciRelObj.getString("direction"));
                        relList.add(relVo);
                        startIndex++;
                        if (ciRelObj.getIntValue("isHidden") == 0) {
                            break;
                        }
                    }
                    ciEntityVo.setRelList(relList);
                    List<CiEntityTopoVo> ciEntityTopoList = ciEntityMapper.getCiEntityForTopo(ciEntityVo);
                    //准备下一次搜索条件
                    ciEntityVo = new CiEntityVo();
                    ciEntityVo.setGlobalAttrFilterList(globalAttrFilterList);
                    List<Long> idList = new ArrayList<>();
                    ciEntityVo.setIdList(idList);
                    for (CiEntityTopoVo ciEntityTopoVo : ciEntityTopoList) {
                        ciTypeIdSet.add(ciEntityTopoVo.getCiType());
                        CiEntityVo tmpCiEntityVo = new CiEntityVo();
                        tmpCiEntityVo.setId(ciEntityTopoVo.getId());
                        tmpCiEntityVo.setCiId(ciEntityTopoVo.getCiId());
                        tmpCiEntityVo.setName(ciEntityTopoVo.getName());
                        tmpCiEntityVo.setCiIcon(ciEntityTopoVo.getCiIcon());
                        tmpCiEntityVo.setTypeId(ciEntityTopoVo.getCiType());
                        ciEntitySet.add(tmpCiEntityVo);
                        if (CollectionUtils.isNotEmpty(ciEntityTopoVo.getRelCiEntityList())) {
                            for (CiEntityTopoVo relCiEntityTopoVo : ciEntityTopoVo.getRelCiEntityList()) {
                                if (relCiEntityTopoVo.getId() != null) {
                                    ciTypeIdSet.add(relCiEntityTopoVo.getCiType());
                                    idList.add(relCiEntityTopoVo.getId());
                                    CiEntityVo tmpRelCiEntityVo = new CiEntityVo();
                                    tmpRelCiEntityVo.setId(relCiEntityTopoVo.getId());
                                    tmpRelCiEntityVo.setCiId(relCiEntityTopoVo.getCiId());
                                    tmpRelCiEntityVo.setName(relCiEntityTopoVo.getName());
                                    tmpRelCiEntityVo.setCiIcon(relCiEntityTopoVo.getCiIcon());
                                    tmpRelCiEntityVo.setTypeId(relCiEntityTopoVo.getCiType());
                                    ciEntitySet.add(tmpRelCiEntityVo);

                                    RelEntityVo tmpRelEntityVo = new RelEntityVo();
                                    tmpRelEntityVo.setRelId(relCiEntityTopoVo.getRelId());
                                    tmpRelEntityVo.setDirection(relCiEntityTopoVo.getDirection());
                                    if (relCiEntityTopoVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                        tmpRelEntityVo.setFromCiEntityId(ciEntityTopoVo.getId());
                                        tmpRelEntityVo.setToCiEntityId(relCiEntityTopoVo.getId());
                                    } else {
                                        tmpRelEntityVo.setToCiEntityId(ciEntityTopoVo.getId());
                                        tmpRelEntityVo.setFromCiEntityId(relCiEntityTopoVo.getId());
                                    }
                                    relEntitySet.add(tmpRelEntityVo);
                                }
                            }
                        }
                    }
                    if (CollectionUtils.isEmpty(idList)) {
                        break;
                    }
                }
            }
        }
        // 开始绘制dot图
        if (!ciEntitySet.isEmpty()) {
            Graphviz.Builder gb = new Graphviz.Builder(LayoutType.get(layout));
            for (CiTypeVo ciTypeVo : ciTypeList) {
                if (ciTypeIdSet.contains(ciTypeVo.getId())) {
                    Layer.Builder lb = new Layer.Builder("CiType" + ciTypeVo.getId());
                    lb.withLabel(ciTypeVo.getName());
                    for (CiEntityVo ciEntityVo : ciEntitySet) {
                        if (ciEntityVo.getTypeId().equals(ciTypeVo.getId())) {
                            Node.Builder nb =
                                    new Node.Builder("CiEntity_" + ciEntityVo.getId());
                            nb.withHeight("0.85");
                            nb.addClass("CiEntity_" + ciEntityVo.getCiId() + "_" + ciEntityVo.getId());// 必须按照这个格式写，前端会通过下划线来提取ciid和cientityid
                            if (StringUtils.isNotBlank(ciEntityVo.getName())) {
                                nb.withTooltip(ciEntityVo.getName())
                                        .withLabel(ciEntityVo.getName());
                            } else {
                                nb.withLabel("-");
                            }
                            nb.withImage(ciEntityVo.getCiIcon());
                            if (ciEntityId.equals(ciEntityVo.getId())) {
                                nb.withFontColor("red")
                                        .addClass("cinode").addClass("corenode");
                            } else {
                                nb.addClass("cinode").addClass("normalnode");
                            }
                            Node node = nb.build();
                            lb.addNode(node);

                            ciEntityNodeSet.add("CiEntity_" + ciEntityVo.getId());

                            //根据分组属性计算分组
                           /* if (CollectionUtils.isNotEmpty(clusterAttrList)) {
                                for (String clusterAttr : clusterAttrList) {
                                    if (clusterAttr.contains(ciEntityVo.getCiName() + "#")) {
                                        String attrname = clusterAttr.split("#")[1];
                                        for (AttrEntityVo attrEntityVo : ciEntityVo.getAttrEntityList()) {
                                            if (attrEntityVo.getAttrName().equals(attrname)) {
                                                String valueMd5 = Md5Util.encryptMD5(attrEntityVo.getValueStr());
                                                if (!clusterMap.containsKey(valueMd5)) {
                                                    clusterMap.put(valueMd5, new Cluster.Builder("cluster_" + valueMd5).withLabel(attrEntityVo.getAttrLabel() + ":" + attrEntityVo.getValueStr()));
                                                }
                                                clusterMap.get(valueMd5).addNode(node);
                                            }
                                        }
                                    }
                                }
                            }*/

                        }
                    }
                    gb.addLayer(lb.build());
                }
            }
            if (!relEntitySet.isEmpty()) {
                for (RelEntityVo relEntityVo : relEntitySet) {
                    if (ciEntityNodeSet.contains("CiEntity_" + relEntityVo.getFromCiEntityId())
                            && ciEntityNodeSet.contains("CiEntity_" + relEntityVo.getToCiEntityId())) {
                        Link.Builder lb = new Link.Builder(
                                "CiEntity_" + relEntityVo.getFromCiEntityId(),
                                "CiEntity_" + relEntityVo.getToCiEntityId());
                        lb.withLabel(relEntityVo.getRelTypeName());
                        lb.setFontSize(9);
                        gb.addLink(lb.build());
                    }
                }
            }

            //测试分组代码
            /*if (MapUtils.isNotEmpty(clusterMap)) {
                for (String key : clusterMap.keySet()) {
                    Cluster.Builder cb = clusterMap.get(key);
                    cb.withStyle("filled");
                    gb.addCluster(cb.build());
                }
            }*/
            List<CiEntityVo> inspectCiEntityList = new ArrayList<>();
            List<CiEntityVo> monitorCiEntityList = new ArrayList<>();
            for (CiEntityVo ciEntityVo : ciEntitySet) {
                if (StringUtils.isNotBlank(ciEntityVo.getInspectStatus())) {
                    CiEntityVo c = new CiEntityVo();
                    c.setId(ciEntityVo.getId());
                    c.setInspectStatus(ciEntityVo.getInspectStatus());
                    inspectCiEntityList.add(c);
                }
                if (StringUtils.isNotBlank(ciEntityVo.getMonitorStatus())) {
                    CiEntityVo c = new CiEntityVo();
                    c.setId(ciEntityVo.getId());
                    c.setMonitorStatus(ciEntityVo.getMonitorStatus());
                    monitorCiEntityList.add(ciEntityVo);
                }
            }


            String dot = gb.build().toString();
            //System.out.println(dot);
            if (logger.isDebugEnabled()) {
                logger.debug(dot);
            }
            returnObj.put("inspectCiEntityList", inspectCiEntityList);
            returnObj.put("monitorCiEntityList", monitorCiEntityList);
            returnObj.put("dot", dot);
        }
        if (CollectionUtils.isNotEmpty(containRelIdSet)) {
            returnObj.put("relList", relMapper.getRelByIdList(new ArrayList<>(containRelIdSet)));
        }
        return returnObj;
    }
}
