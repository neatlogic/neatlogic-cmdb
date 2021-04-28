/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.topo;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dot.*;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiTopoApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(GetCiTopoApi.class);

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/topo/ci";
    }

    @Override
    public String getName() {
        return "获取模型拓扑";
    }

    @Override
    public String getConfig() {
        return null;
    }

    public static void main(String[] ag) {
        Graphviz.Builder gb = new Graphviz.Builder();
        Random random = new Random();
        List<Node> nodeList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Layer.Builder lb = new Layer.Builder("CiType" + i);
            lb.withLabel("层" + i);
            for (int j = 1; j <= 3; j++) {
                Node.Builder nb = new Node.Builder("Ci_" + i + "_" + j);
                nb.withLabel("对象" + i + "_" + j);
                nb.withClass("cinode normalnode");
                Node n = nb.build();
                lb.addNode(n);
                if (i == 1) {
                    nodeList.add(n);
                }
                Link.Builder linkb = new Link.Builder("Ci_" + (random.nextInt(3) + 1) + "_" + (random.nextInt(3) + 1), "Ci_" + (random.nextInt(3) + 1) + "_" + (random.nextInt(3) + 1));
                linkb.setFontSize(9);
                gb.addLink(linkb.build());
            }
            gb.addLayer(lb.build());
        }

        //测试分组代码
        Map<String, Cluster.Builder> clusterMap = new HashMap<>();
        Cluster.Builder c = new Cluster.Builder("cluster_abc").withLabel("分组");
        for (Node n : nodeList) {
            c.addNode(n);
        }
        clusterMap.put("abc", c);

        if (MapUtils.isNotEmpty(clusterMap)) {
            for (String key : clusterMap.keySet()) {
                Cluster.Builder cb = clusterMap.get(key);
                cb.withStyle("filled");
                gb.addCluster(cb.build());
            }
        }


        System.out.println(gb.build().toString());
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取模型拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ci = JSONObject.toJavaObject(jsonObj, CiVo.class);
        ci.setIsTypeShowInTopo(1);
        List<CiTypeVo> ciTypeList = ciMapper.searchCiTypeCi(ci);
        List<RelVo> relList = relMapper.getAllRelList();
        Graphviz.Builder gb = new Graphviz.Builder();
        Set<Long> ciIdSet = new HashSet<>();
        for (CiTypeVo ciTypeVo : ciTypeList) {
            if (CollectionUtils.isNotEmpty(ciTypeVo.getCiList())) {
                Layer.Builder lb = new Layer.Builder("CiType" + ciTypeVo.getId());
                lb.withLabel(ciTypeVo.getName());
                for (CiVo ciVo : ciTypeVo.getCiList()) {
                    Node.Builder nb = new Node.Builder("Ci_" + ciVo.getId());
                    nb.withTooltip(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                    nb.withLabel(ciVo.getLabel());
                    nb.withImage(ciVo.getIcon());
                    nb.withClass("cinode normalnode");
                    lb.addNode(nb.build());
                    ciIdSet.add(ciVo.getId());
                }
                gb.addLayer(lb.build());
            }
        }
        //为继承关系添加关联
        for (CiTypeVo ciTypeVo : ciTypeList) {
            if (CollectionUtils.isNotEmpty(ciTypeVo.getCiList())) {
                for (CiVo ciVo : ciTypeVo.getCiList()) {
                    if (ciVo.getParentCiId() != null) {
                        if (ciIdSet.contains(ciVo.getParentCiId()) && ciIdSet.contains(ciVo.getId())) {
                            Link.Builder linkBuilder = new Link.Builder("Ci_" + ciVo.getId(), "Ci_" + ciVo.getParentCiId());
                            linkBuilder.setStyle("dashed");
                            gb.addLink(linkBuilder.build());
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(relList)) {
            for (RelVo relVo : relList) {
                if (ciIdSet.contains(relVo.getFromCiId()) && ciIdSet.contains(relVo.getToCiId())) {
                    Link.Builder lb = new Link.Builder("Ci_" + relVo.getFromCiId(), "Ci_" + relVo.getToCiId());
                    lb.withLabel(relVo.getTypeText());
                    lb.setFontSize(9);
                    gb.addLink(lb.build());
                }
            }
        }
        String dot = gb.build().toString();
        if (logger.isDebugEnabled()) {
            logger.debug(dot);
        }
        return dot;
    }

}
