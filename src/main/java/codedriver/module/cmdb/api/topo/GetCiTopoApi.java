/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.topo;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.framework.graphviz.*;
import codedriver.framework.graphviz.enums.LayoutType;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
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
        Graphviz.Builder gb = new Graphviz.Builder(LayoutType.DOT);
        Random random = new Random();
        List<Node> nodeList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Layer.Builder lb = new Layer.Builder("CiType" + i);
            lb.withLabel("层" + i);
            for (int j = 1; j <= 3; j++) {
                Node.Builder nb = new Node.Builder("Ci_" + i + "_" + j);
                nb.withLabel("对象" + i + "_" + j);
                nb.addClass("cinode").addClass("normalnode");
                Node n = nb.build();
                lb.addNode(n);
                if (i == 1) {
                    nodeList.add(n);
                }
                Link.Builder linkBuilder = new Link.Builder("Ci_" + (random.nextInt(3) + 1) + "_" + (random.nextInt(3) + 1), "Ci_" + (random.nextInt(3) + 1) + "_" + (random.nextInt(3) + 1));
                linkBuilder.setFontSize(9);
                gb.addLink(linkBuilder.build());
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

    @Input({@Param(name = "layout", type = ApiParamType.ENUM, rule = "dot,circo,fdp,neato,osage,patchwork,twopi", isRequired = true),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取模型拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String layout = jsonObj.getString("layout");
        CiVo ci = JSONObject.toJavaObject(jsonObj, CiVo.class);
        ci.setIsTypeShowInTopo(1);
        List<CiTypeVo> ciTypeList = ciMapper.searchCiTypeCi(ci);

        //根据权限去掉没权限查看的模型
        if (!AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY")) {
            for (CiTypeVo ciType : ciTypeList) {
                Iterator<CiVo> itCi = ciType.getCiList().iterator();
                while (itCi.hasNext()) {
                    CiVo ciVo = itCi.next();
                    if (CollectionUtils.isNotEmpty(ciVo.getAuthList())) {
                        if (!CiAuthChecker.hasPrivilege(ciVo.getAuthList(), CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY)) {
                            if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                                itCi.remove();
                            }
                        }
                    } else {
                        if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                            itCi.remove();
                        }
                    }
                }
            }
        }

        List<RelVo> relList = relMapper.getAllRelList();
        Graphviz.Builder gb = new Graphviz.Builder(LayoutType.get(layout));
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
                    nb.addClass("cinode").addClass("normalnode");
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
                            linkBuilder.setStyle("dotted");
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
                    if (relVo.getIsShowInTopo().equals(0)) {
                        lb.setStyle("dashed");
                    }
                    gb.addLink(lb.build());
                }
            }
        }
        String dot = gb.build().toString();
        if (logger.isDebugEnabled()) {
            logger.debug(dot);
        }
        //System.out.println(dot);
        return dot;
    }

}
