/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.topo;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.customview.CustomViewCiVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewLinkVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.Md5Util;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiTypeMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dot.*;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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

    @Autowired
    private CiEntityService ciEntityService;

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

    @Input({@Param(name = "customViewId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "自动展开关系层数，默认是1")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取自定义视图配置项拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(jsonObj, CustomViewConditionVo.class);
        CustomViewVo customViewVo = customViewDataService.getCustomViewCiEntityById(customViewConditionVo);
        //提取cientityid，补充层次等属性
        Set<Long> ciEntityIdList = new HashSet<>();
        for (CustomViewCiVo ciVo : customViewVo.getCiList()) {
            if (CollectionUtils.isNotEmpty(ciVo.getCiEntityList())) {
                ciEntityIdList.addAll(ciVo.getCiEntityList().stream().map(cientity -> cientity.getLong("id")).collect(Collectors.toList()));
            }
        }
        List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(new ArrayList<>(ciEntityIdList));

        Map<Long, CiEntityVo> ciEntityMap = new HashMap<>();
        // 所有需要绘制的层次
        Set<Long> ciTypeIdSet = new HashSet<>();
        for (CiEntityVo ciEntityVo : ciEntityList) {
            ciTypeIdSet.add(ciEntityVo.getTypeId());
            ciEntityMap.put(ciEntityVo.getId(), ciEntityVo);
        }
        // 所有需要绘制的关系
        Set<RelEntityVo> relEntitySet = new HashSet<>();

        Long ciEntityId = jsonObj.getLong("ciEntityId");
        //Long ciId = jsonObj.getLong("ciId");
        //分组属性
        List<String> clusterAttrList = new ArrayList<>();
        clusterAttrList.add("tomcat#port");
        clusterAttrList.add("hardware#brand");
        clusterAttrList.add("system#env");
        Map<String, Cluster.Builder> clusterMap = new HashMap<>();
        // 先搜索出所有层次，因为需要按照层次顺序展示
        CiTypeVo pCiTypeVo = new CiTypeVo();
        pCiTypeVo.setIsShowInTopo(1);
        List<CiTypeVo> ciTypeList = ciTypeMapper.searchCiType(pCiTypeVo);
        Set<Long> canShowCiTypeIdSet = new HashSet<>();
        for (CiTypeVo ciTypeVo : ciTypeList) {
            canShowCiTypeIdSet.add(ciTypeVo.getId());
        }

        // 开始绘制dot图
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            Graphviz.Builder gb = new Graphviz.Builder();
            for (CiTypeVo ciTypeVo : ciTypeList) {
                if (ciTypeIdSet.contains(ciTypeVo.getId())) {
                    Layer.Builder lb = new Layer.Builder("CiType" + ciTypeVo.getId());
                    lb.withLabel(ciTypeVo.getName());
                    for (CiEntityVo ciEntityVo : ciEntityList) {
                        if (ciEntityVo.getTypeId().equals(ciTypeVo.getId())) {
                            Node.Builder nb =
                                    new Node.Builder("CiEntity_" + ciEntityVo.getCiId() + "_" + ciEntityVo.getId());// 必须按照这个格式写，前端会通过下划线来提取ciid和cientityid
                            nb.withTooltip(ciEntityVo.getName());
                            nb.withLabel(ciEntityVo.getName());
                            nb.withImage(ciEntityVo.getCiIcon());
                            if (ciEntityId.equals(ciEntityVo.getId())) {
                                nb.withFontColor("red");
                                nb.withClass("cinode corenode");
                            } else {
                                nb.withClass("cinode normalnode");
                            }
                            Node node = nb.build();
                            lb.addNode(node);


                            //根据分组属性计算分组
                            if (CollectionUtils.isNotEmpty(clusterAttrList)) {
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
                            }

                        }
                    }
                    gb.addLayer(lb.build());
                }
            }
            if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
                for (CustomViewLinkVo linkVo : customViewVo.getLinkList()) {
                    CustomViewCiVo fromCi = customViewVo.getCustomCiByUuid(linkVo.getFromCustomViewCiUuid());
                    CustomViewCiVo toCi = customViewVo.getCustomCiByUuid(linkVo.getToCustomViewCiUuid());
                    if (CollectionUtils.isNotEmpty(fromCi.getCiEntityList()) && CollectionUtils.isNotEmpty(toCi.getCiEntityList())) {
                        for (JSONObject fromCiEntityObj : fromCi.getCiEntityList()) {
                            for (JSONObject toCiEntityObj : toCi.getCiEntityList()) {
                                Link.Builder lb = new Link.Builder(
                                        "CiEntity_" + fromCi.getCiId() + "_" + fromCiEntityObj.getLong("id"),
                                        "CiEntity_" + toCi.getCiId() + "_" + toCiEntityObj.getLong("id"));
                                if (StringUtils.isNotBlank(linkVo.getName())) {
                                    lb.withLabel(linkVo.getName());
                                }
                                lb.setFontSize(9);
                                gb.addLink(lb.build());
                            }
                        }
                    }
                }
            }

            //测试分组代码
            if (MapUtils.isNotEmpty(clusterMap)) {
                for (String key : clusterMap.keySet()) {
                    Cluster.Builder cb = clusterMap.get(key);
                    cb.withStyle("filled");
                    gb.addCluster(cb.build());
                }
            }


            String dot = gb.build().toString();
            if (logger.isDebugEnabled()) {
                logger.debug(dot);
            }
            //System.out.println(dot);
            return dot;
        }
        return "";
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
        System.out.println(set.size());
    }

}