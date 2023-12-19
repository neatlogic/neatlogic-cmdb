package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiCatalogAndCiForTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiCatalogService ciCatalogService;

    @Override
    public String getName() {
        return "nmcac.listcicatalogandcifortreeapi.getname";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword")
    })
    @Output({
            @Param(name = "tbodyList", explode = CiCatalogVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcac.listcicatalogandcifortreeapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String keyword = paramObj.getString("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase();
        }
        List<CiCatalogNodeVo> allNodeList = ciCatalogService.getAllCiCatalogList();
        if (CollectionUtils.isEmpty(allNodeList)) {
            return allNodeList;
        }
        CiCatalogNodeVo rootNode = allNodeList.get(0);
        Map<Long, CiCatalogNodeVo> id2NodeMap = new HashMap<>();
        List<CiCatalogNodeVo> matchKeywordCiCatalogNodeList = new ArrayList<>();
        for (CiCatalogNodeVo node : allNodeList) {
            id2NodeMap.put(node.getId(), node);
            if (StringUtils.isNotBlank(keyword)) {
                if (node.getName().toLowerCase().contains(keyword)) {
                    matchKeywordCiCatalogNodeList.add(node);
                }
            }
        }
        List<CiCatalogNodeVo> noCatalogCiNodeList = new ArrayList<>();
        List<CiCatalogNodeVo> ciNodeList = new ArrayList<>();
        List<CiVo> ciList = ciMapper.getAllCi(null);
        for (CiVo ciVo : ciList) {
            if (StringUtils.isNotBlank(keyword)) {
                if (!ciVo.getName().toLowerCase().contains(keyword) && !ciVo.getLabel().toLowerCase().contains(keyword)) {
                    continue;
                }
            }
            CiCatalogNodeVo ciNode = new CiCatalogNodeVo();
            ciNode.setId(ciVo.getId());
            ciNode.setName(ciVo.getLabel() + "(" + ciVo.getName() + ")");
            ciNode.setParentId(ciVo.getCatalogId());
            ciNode.setType(CiCatalogNodeVo.CI);
            if (ciVo.getCatalogId() == null) {
                noCatalogCiNodeList.add(ciNode);
                continue;
            }
            CiCatalogNodeVo node = id2NodeMap.get(ciVo.getCatalogId());
            if (node == null) {
                noCatalogCiNodeList.add(ciNode);
                continue;
            }
            if (StringUtils.isNotBlank(keyword)) {
                matchKeywordCiCatalogNodeList.add(node);
            }
            ciNodeList.add(ciNode);
        }
        List<CiCatalogNodeVo> catalogList = new ArrayList<>();
        for (CiCatalogNodeVo node : allNodeList) {
            node.setType(CiCatalogNodeVo.CATALOG);
            if (CollectionUtils.isNotEmpty(matchKeywordCiCatalogNodeList)) {
                for (CiCatalogNodeVo matchKeywordCiCatalogNode : matchKeywordCiCatalogNodeList) {
                    if (node.getLft() <= matchKeywordCiCatalogNode.getLft() && node.getRht() >= matchKeywordCiCatalogNode.getRht()) {
                        catalogList.add(node);
                    }
                }
            } else {
                catalogList.add(node);
            }
        }
        for (CiCatalogNodeVo node : catalogList) {
            CiCatalogNodeVo parent = id2NodeMap.get(node.getParentId());
            if (parent != null) {
                parent.addChild(node);
            }
        }
        for (CiCatalogNodeVo node : ciNodeList) {
            CiCatalogNodeVo parent = id2NodeMap.get(node.getParentId());
            if (parent != null) {
                parent.addChild(node);
            }
        }
        for (CiCatalogNodeVo node : noCatalogCiNodeList) {
            rootNode.addChild(node);
        }
        return rootNode.getChildren();
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalogandci/listtree";
    }
}
