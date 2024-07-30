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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewCiVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewLinkVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.graphviz.Graphviz;
import neatlogic.framework.graphviz.Layer;
import neatlogic.framework.graphviz.Link;
import neatlogic.framework.graphviz.Node;
import neatlogic.framework.graphviz.enums.LayoutType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCustomViewDataCiEntityTopoApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(GetCustomViewDataCiEntityTopoApi.class);

    @Resource
    private CustomViewDataService customViewDataService;


    @Resource
    private CiEntityMapper ciEntityMapper;


    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/customview/data/cientity/topo";
    }

    @Override
    public String getName() {
        return "获取自定义视图配置项拓扑";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "layout", type = ApiParamType.ENUM, rule = "dot,circo,fdp,neato,osage,patchwork,twopi", isRequired = true),
            @Param(name = "customViewId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "自动展开关系层数，默认是1")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取自定义视图配置项拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String layout = jsonObj.getString("layout");
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(jsonObj, CustomViewConditionVo.class);
        CustomViewVo customViewVo = customViewDataService.getCustomViewCiEntityById(customViewConditionVo);
        //提取cientityid，补充层次等属性
        Set<Long> ciEntityIdList = new HashSet<>();
        Map<Long, Boolean> isHiddenMap = new HashMap<>();
        for (CustomViewCiVo ciVo : customViewVo.getCiList()) {
            if (ciVo.getIsHidden().equals(0) && CollectionUtils.isNotEmpty(ciVo.getCiEntityList())) {
                ciEntityIdList.addAll(ciVo.getCiEntityList().stream().map(cientity -> cientity.getLong("id")).collect(Collectors.toList()));
            }
            isHiddenMap.put(ciVo.getCiId(), ciVo.getIsHidden().equals(1));
        }
        if (CollectionUtils.isEmpty(ciEntityIdList)) {
            //如果没有数据，至少查出自己的信息
            ciEntityIdList.add(jsonObj.getLong("ciEntityId"));
        }
        List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(new ArrayList<>(ciEntityIdList));
        // 所有需要绘制的层次
        Set<Long> ciTypeIdSet = new HashSet<>();
        for (CiEntityVo ciEntityVo : ciEntityList) {
            if (isHiddenMap.containsKey(ciEntityVo.getCiId()) && !isHiddenMap.get(ciEntityVo.getCiId())) {
                ciTypeIdSet.add(ciEntityVo.getTypeId());
            }
        }
        JSONObject returnObj = new JSONObject();
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        //Long ciId = jsonObj.getLong("ciId");
        //分组属性
        /*List<String> clusterAttrList = new ArrayList<>();
        clusterAttrList.add("tomcat#port");
        clusterAttrList.add("hardware#brand");
        clusterAttrList.add("system#env");
        Map<String, Cluster.Builder> clusterMap = new HashMap<>();*/
        // 先搜索出所有层次，因为需要按照层次顺序展示
        CiTypeVo pCiTypeVo = new CiTypeVo();
        //视图层次不受模型类型限制
        //pCiTypeVo.setIsShowInTopo(1);
        List<CiTypeVo> ciTypeList = ciTypeMapper.searchCiType(pCiTypeVo);

        //记录已经绘制的配置项
        Set<String> ciEntityNodeSet = new HashSet<>();

        // 开始绘制dot图
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            Graphviz.Builder gb = new Graphviz.Builder(LayoutType.get(layout));
            for (CiTypeVo ciTypeVo : ciTypeList) {
                if (ciTypeIdSet.contains(ciTypeVo.getId())) {
                    Layer.Builder lb = new Layer.Builder("CiType" + ciTypeVo.getId());
                    lb.withLabel(ciTypeVo.getName());
                    for (CiEntityVo ciEntityVo : ciEntityList) {
                        if (ciEntityVo.getTypeId().equals(ciTypeVo.getId())) {
                            Node.Builder nb =
                                    new Node.Builder("CiEntity_" + ciEntityVo.getId());
                            nb.addClass("CiEntity_" + ciEntityVo.getCiId() + "_" + ciEntityVo.getId());// 必须按照这个格式写，前端会通过下划线来提取ciid和cientityid
                            nb.withTooltip(ciEntityVo.getName());
                            nb.withHeight("0.85");
                            nb.withLabel(StringUtils.isNotBlank(ciEntityVo.getName()) ? ciEntityVo.getName() : "无名配置项");
                            nb.withImage(ciEntityVo.getCiIcon());
                            if (ciEntityId.equals(ciEntityVo.getId())) {
                                nb.withFontColor("red");
                                nb.addClass("cinode").addClass("corenode");
                            } else {
                                nb.addClass("cinode").addClass("normalnode");
                            }
                            Node node = nb.build();
                            lb.addNode(node);
                            ciEntityNodeSet.add("CiEntity_" + ciEntityVo.getId());

                            //根据分组属性计算分组
                            /*if (CollectionUtils.isNotEmpty(clusterAttrList)) {
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
            //如果存在隐藏模型，则需要修正关系列表，裁剪隐藏模型的关系
            if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
                for (CustomViewCiVo ciVo : customViewVo.getCiList()) {
                    if (ciVo.getIsHidden().equals(1)) {
                        List<CustomViewLinkVo> tempLinkList = new ArrayList<>();
                        Iterator<CustomViewLinkVo> itLink = customViewVo.getLinkList().iterator();
                        while (itLink.hasNext()) {
                            CustomViewLinkVo link = itLink.next();
                            if (link.getFromCustomViewCiUuid().equals(ciVo.getUuid()) || link.getToCustomViewCiUuid().equals(ciVo.getUuid())) {
                                itLink.remove();
                                CustomViewLinkVo newLink = new CustomViewLinkVo();
                                if (link.getFromCustomViewCiUuid().equals(ciVo.getUuid())) {
                                    newLink.setToCustomViewCiUuid(link.getToCustomViewCiUuid());
                                } else {
                                    newLink.setFromCustomViewCiUuid(link.getFromCustomViewCiUuid());
                                }
                                tempLinkList.add(newLink);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(tempLinkList)) {
                            List<CustomViewLinkVo> fromTempLinkList = tempLinkList.stream().filter(d -> StringUtils.isNotBlank(d.getFromCustomViewCiUuid())).collect(Collectors.toList());
                            List<CustomViewLinkVo> toTempLinkList = tempLinkList.stream().filter(d -> StringUtils.isNotBlank(d.getToCustomViewCiUuid())).collect(Collectors.toList());
                            for (CustomViewLinkVo tmpFromLink : fromTempLinkList) {
                                for (CustomViewLinkVo tmpToLink : toTempLinkList) {
                                    CustomViewLinkVo newLink = new CustomViewLinkVo();
                                    newLink.setFromCustomViewCiUuid(tmpFromLink.getFromCustomViewCiUuid());
                                    newLink.setToCustomViewCiUuid(tmpToLink.getToCustomViewCiUuid());
                                    customViewVo.getLinkList().add(newLink);
                                }
                            }
                        }
                    }
                }
            }
            Set<String> uniqueLinkSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
                for (CustomViewLinkVo linkVo : customViewVo.getLinkList()) {
                    CustomViewCiVo fromCi = customViewVo.getCustomCiByUuid(linkVo.getFromCustomViewCiUuid());
                    CustomViewCiVo toCi = customViewVo.getCustomCiByUuid(linkVo.getToCustomViewCiUuid());
                    if (CollectionUtils.isNotEmpty(fromCi.getCiEntityList()) && CollectionUtils.isNotEmpty(toCi.getCiEntityList())) {
                        for (JSONObject fromCiEntityObj : fromCi.getCiEntityList()) {
                            for (JSONObject toCiEntityObj : toCi.getCiEntityList()) {
                                if (ciEntityNodeSet.contains("CiEntity_" + fromCiEntityObj.getLong("id")) &&
                                        ciEntityNodeSet.contains("CiEntity_" + toCiEntityObj.getLong("id")) && !uniqueLinkSet.contains(fromCiEntityObj.getLong("id") + "-" + toCiEntityObj.getLong("id"))) {
                                    Link.Builder lb = new Link.Builder(
                                            "CiEntity_" + fromCiEntityObj.getLong("id"),
                                            "CiEntity_" + toCiEntityObj.getLong("id"));
                                    if (StringUtils.isNotBlank(linkVo.getName())) {
                                        lb.withLabel(linkVo.getName());
                                    }
                                    lb.setFontSize(9);
                                    gb.addLink(lb.build());
                                    uniqueLinkSet.add(fromCiEntityObj.getLong("id") + "-" + toCiEntityObj.getLong("id"));
                                }
                            }
                        }
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
            if (logger.isDebugEnabled()) {
                logger.debug(dot);
            }
            returnObj.put("dot", dot);
        }
        return returnObj;
    }


}
