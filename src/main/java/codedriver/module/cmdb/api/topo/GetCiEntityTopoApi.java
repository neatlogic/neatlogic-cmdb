/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.topo;

import codedriver.framework.cmdb.constvalue.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.Md5Util;
import codedriver.module.cmdb.dao.mapper.ci.CiTypeMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dot.*;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiEntityTopoApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(GetCiEntityTopoApi.class);

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/topo/cientity";
    }

    @Override
    public String getName() {
        return "获取配置项拓扑";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "level", type = ApiParamType.INTEGER, desc = "自动展开关系层数，默认是1")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取配置项拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        // 所有需要绘制的配置项
        Set<CiEntityVo> ciEntitySet = new HashSet<>();
        // 所有需要绘制的层次
        Set<Long> ciTypeIdSet = new HashSet<>();
        // 所有需要绘制的关系
        Set<RelEntityVo> relEntitySet = new HashSet<>();

        Long ciEntityId = jsonObj.getLong("ciEntityId");
        //分组属性
        List<String> clusterAttrList = new ArrayList<>();
        clusterAttrList.add("tomcat#port");
        clusterAttrList.add("hardware#brand");
        clusterAttrList.add("system#env");
        Map<String, Cluster.Builder> clusterMap = new HashMap<>();
        // 先搜索出所有层次，因为需要按照层次顺序展示
        List<CiTypeVo> ciTypeList = ciTypeMapper.searchCiType(null);
        Integer level = jsonObj.getInteger("level");
        if (level == null) {
            level = 1;
        }

        // 每一层需要搜索关系的节点列表
        Set<Long> ciEntityIdList = new LinkedHashSet<>();
        ciEntityIdList.add(ciEntityId);
        for (int l = 0; l <= level; l++) {
            if (!ciEntityIdList.isEmpty()) {
                List<Long> tmpList = new ArrayList<>(ciEntityIdList);
                // 获取当前层次配置项详细信息
                List<CiEntityVo> ciEntityList = ciEntityMapper.searchCiEntityByIdList(tmpList);
                if (CollectionUtils.isEmpty(ciEntityList)) {
                    // 如果找不到配置项，则退出循环
                    break;
                }
                List<AttrEntityVo> attrEntityList = attrEntityMapper.searchAttrEntityByCiEntityIdList(tmpList, null);

               /* for (CiEntityVo entity : ciEntityList) {
                    entity.setAttrEntityList(attrEntityList.stream()
                            .filter(attr -> attr.getCiEntityId().equals(entity.getId())).collect(Collectors.toList()));
                    ciEntitySet.add(entity);
                    ciTypeIdSet.add(entity.getTypeId());
                }*/
                // 获取当前层次配置项所有关系(包括上下游)
                List<RelEntityVo> relEntityList = relEntityMapper.searchRelEntityByCiEntityIdList(tmpList, null);
                ciEntityIdList.clear();// 清空查询列表，为下一层搜索做准备
                for (RelEntityVo relEntityVo : relEntityList) {
                    relEntitySet.add(relEntityVo);
                    // 检查关系中的对端配置项是否已经存在，不存在可进入下一次搜索
                    if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        // 不存在的配置项可以进入下一次检索
                        if (!ciEntitySet.contains(new CiEntityVo(relEntityVo.getToCiEntityId()))) {
                            ciEntityIdList.add(relEntityVo.getToCiEntityId());
                        }
                    } else if (relEntityVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                        // 不存在的配置项可以进入下一次检索
                        if (!ciEntitySet.contains(new CiEntityVo(relEntityVo.getFromCiEntityId()))) {
                            ciEntityIdList.add(relEntityVo.getFromCiEntityId());
                        }
                    }
                }
                if (ciEntityIdList.isEmpty()) {
                    // 如果已经没有任何关系，退出循环
                    break;
                }
            }
        }
        // 开始绘制dot图
        if (!ciEntitySet.isEmpty()) {
            Graphviz.Builder gb = new Graphviz.Builder();
            for (CiTypeVo ciTypeVo : ciTypeList) {
                if (ciTypeIdSet.contains(ciTypeVo.getId())) {
                    Layer.Builder lb = new Layer.Builder("CiType" + ciTypeVo.getId());
                    lb.withLabel(ciTypeVo.getName());
                    for (CiEntityVo ciEntityVo : ciEntitySet) {
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
            if (!relEntitySet.isEmpty()) {
                for (RelEntityVo relEntityVo : relEntitySet) {
                    if (ciEntitySet.contains(new CiEntityVo(relEntityVo.getFromCiEntityId()))
                            && ciEntitySet.contains(new CiEntityVo(relEntityVo.getToCiEntityId()))) {
                        Link.Builder lb = new Link.Builder(
                                "CiEntity_" + relEntityVo.getFromCiId() + "_" + relEntityVo.getFromCiEntityId(),
                                "CiEntity_" + relEntityVo.getToCiId() + "_" + relEntityVo.getToCiEntityId());
                        lb.withLabel(relEntityVo.getRelTypeName());
                        lb.setFontSize(9);
                        gb.addLink(lb.build());
                    }
                }
            }

            //测试分组代码
            if (MapUtils.isNotEmpty(clusterMap)) {
                for (String key : clusterMap.keySet()) {
                    Cluster.Builder cb = clusterMap.get(key);
                    cb.withStyle("filled");
                    Cluster c = cb.build();
                    gb.addCluster(cb.build());
                }
            }


            String dot = gb.build().toString();
            if (logger.isDebugEnabled()) {
                logger.debug(dot);
            }
            System.out.println(dot);
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
