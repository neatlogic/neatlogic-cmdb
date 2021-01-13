package codedriver.module.cmdb.api.topo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dot.Graphviz;
import codedriver.module.cmdb.dot.Layer;
import codedriver.module.cmdb.dot.Link;
import codedriver.module.cmdb.dot.Node;
import codedriver.module.cmdb.dto.ci.CiTypeVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;

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

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
        @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id")})
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取模型拓扑接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiVo ci = JSONObject.toJavaObject(jsonObj, CiVo.class);
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
