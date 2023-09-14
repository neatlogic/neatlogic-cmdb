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

package neatlogic.module.cmdb.api.topo;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrFilterVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.graphviz.*;
import neatlogic.framework.graphviz.enums.LayoutType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
    private CiEntityService ciEntityService;


    @Resource
    private CiTypeMapper ciTypeMapper;

    @Resource
    private RelMapper relMapper;

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

    @Input({@Param(name = "layout", type = ApiParamType.ENUM, rule = "dot,circo,fdp,neato,osage,patchwork,twopi", isRequired = true),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.cientityid"),
            @Param(name = "globalAttrFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.globalattrfilterlist"),
            @Param(name = "disableRelList", type = ApiParamType.JSONARRAY, desc = "nmcat.getcientitytopoapi.input.param.desc.disablerellist"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "nmcat.getcientitytopoapi.input.param.desc.level")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "nmcat.getcientitytopoapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String layout = jsonObj.getString("layout");
        // 所有需要绘制的配置项
        Set<CiEntityVo> ciEntitySet = new HashSet<>();
        // 所有需要绘制的层次
        Set<Long> ciTypeIdSet = new HashSet<>();
        // 所有需要绘制的关系
        Set<RelEntityVo> relEntitySet = new HashSet<>();
        //记录已经绘制的配置项
        Set<String> ciEntityNodeSet = new HashSet<>();

        Long ciEntityId = jsonObj.getLong("ciEntityId");
        JSONArray disableRelObjList = jsonObj.getJSONArray("disableRelList");
        JSONArray globalAttrFilterObjList = jsonObj.getJSONArray("globalAttrFilterList");
        List<GlobalAttrFilterVo> globalAttrFilterList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(globalAttrFilterObjList)) {
            for (int i = 0; i < globalAttrFilterObjList.size(); i++) {
                GlobalAttrFilterVo globalAttrFilterVo = JSONObject.toJavaObject(globalAttrFilterObjList.getJSONObject(i), GlobalAttrFilterVo.class);
                globalAttrFilterList.add(globalAttrFilterVo);
            }
        }
        Set<Long> disableRelIdList = new HashSet<>();
        Set<Long> containRelIdSet = new HashSet<>();
        JSONObject returnObj = new JSONObject();
        if (CollectionUtils.isNotEmpty(disableRelObjList)) {
            for (int i = 0; i < disableRelObjList.size(); i++) {
                disableRelIdList.add(disableRelObjList.getLong(i));
            }
        }
        //Long ciId = jsonObj.getLong("ciId");
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

        // 每一层需要搜索关系的节点列表
        Map<Long, List<Long>> ciCiEntityIdMap = new HashMap<>();
        ciCiEntityIdMap.put(jsonObj.getLong("ciId"), new ArrayList<Long>() {{
            this.add(ciEntityId);
        }});
        for (int l = 0; l <= level; l++) {
            if (MapUtils.isNotEmpty(ciCiEntityIdMap)) {
                Map<Long, List<Long>> tmpCiCiEntityIdMap = new HashMap<>();
                for (Long ciId : ciCiEntityIdMap.keySet()) {
                    // 获取当前层次配置项详细信息
                    CiEntityVo pCiEntityVo = new CiEntityVo();
                    pCiEntityVo.setIdList(ciCiEntityIdMap.get(ciId));
                    pCiEntityVo.setCiId(ciId);
                    pCiEntityVo.setMaxRelEntityCount(50L);
                    pCiEntityVo.setGlobalAttrFilterList(globalAttrFilterList);
                    //不需要多余的属性
                    pCiEntityVo.setAttrIdList(new ArrayList<Long>() {{
                        this.add(0L);
                    }});
                    List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                    if (CollectionUtils.isNotEmpty(ciEntityList)) {
                        // 获取当前层次配置项所有关系(包括上下游)
                        for (CiEntityVo ciEntityVo : ciEntityList) {
                            if (canShowCiTypeIdSet.contains(ciEntityVo.getTypeId())) {
                                ciEntitySet.add(ciEntityVo);
                                ciTypeIdSet.add(ciEntityVo.getTypeId());
                            }
                            for (RelEntityVo relEntityVo : ciEntityVo.getRelEntityList()) {
                                RelTypeVo relTypeVo = relMapper.getRelTypeByRelId(relEntityVo.getRelId());
                                //判断关系类型是否展示TOPO
                                if (relTypeVo != null && relTypeVo.getIsShowInTopo().equals(1)) {
                                    //记录所有存在数据的关系
                                    containRelIdSet.add(relEntityVo.getRelId());
                                    if (CollectionUtils.isEmpty(disableRelIdList) || disableRelIdList.stream().noneMatch(r -> r.equals(relEntityVo.getRelId()))) {
                                        relEntitySet.add(relEntityVo);
                                        // 检查关系中的对端配置项是否已经存在，不存在可进入下一次搜索
                                        if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                            if (!tmpCiCiEntityIdMap.containsKey(relEntityVo.getToCiId())) {
                                                tmpCiCiEntityIdMap.put(relEntityVo.getToCiId(), new ArrayList<>());
                                            }
                                            tmpCiCiEntityIdMap.get(relEntityVo.getToCiId()).add(relEntityVo.getToCiEntityId());
                                        } else if (relEntityVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                                            if (!tmpCiCiEntityIdMap.containsKey(relEntityVo.getFromCiId())) {
                                                tmpCiCiEntityIdMap.put(relEntityVo.getFromCiId(), new ArrayList<>());
                                            }
                                            tmpCiCiEntityIdMap.get(relEntityVo.getFromCiId()).add(relEntityVo.getFromCiEntityId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ciCiEntityIdMap = tmpCiCiEntityIdMap;
            } else {
                break;
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


            String dot = gb.build().toString();
            //System.out.println(dot);
            if (logger.isDebugEnabled()) {
                logger.debug(dot);
            }
            returnObj.put("dot", dot);
        }
        if (CollectionUtils.isNotEmpty(containRelIdSet)) {
            returnObj.put("relList", relMapper.getRelByIdList(new ArrayList<>(containRelIdSet)));
        }
        return returnObj;
    }

    public static void main(String[] argv) {
        RelEntityVo relEntityVo = new RelEntityVo();
        relEntityVo.setRelId(1L);
        relEntityVo.setFromCiEntityId(1L);
        relEntityVo.setToCiEntityId(2L);
        relEntityVo.setDirection("from");

        RelEntityVo relEntityVo2 = new RelEntityVo();
        relEntityVo2.setRelId(1L);
        relEntityVo2.setFromCiEntityId(1L);
        relEntityVo2.setToCiEntityId(2L);
        relEntityVo2.setDirection("from");
        Set<RelEntityVo> set = new HashSet<>();
        set.add(relEntityVo);
        set.add(relEntityVo2);
        //System.out.println(set.size());
    }

}
