package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiCatalogForTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiCatalogService ciCatalogService;

    @Override
    public String getName() {
        return "nmcac.listcicatalogfortreeapi.getname";
    }

    @Input({})
    @Output({
            @Param(name = "tbodyList", explode = CiCatalogVo[].class, desc = "common.tbodylist")
    })
    @Description(desc = "nmcac.listcicatalogfortreeapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<CiCatalogNodeVo> allNodeList = ciCatalogService.getAllCiCatalogList();
        if (CollectionUtils.isEmpty(allNodeList)) {
            return allNodeList;
        }
        Map<Long, List<CiVo>> catalogId2CiListMap = new HashMap<>();
        List<CiVo> ciList = ciMapper.getAllCi(null);
        for (CiVo ciVo : ciList) {
            if (ciVo.getCatalogId() != null) {
                catalogId2CiListMap.computeIfAbsent(ciVo.getCatalogId(), key -> new ArrayList<>()).add(ciVo);
            }
        }
        CiCatalogNodeVo rootNode = allNodeList.get(0);
        Map<Long, CiCatalogNodeVo> id2NodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (CiCatalogNodeVo node : allNodeList) {
            node.setType(CiCatalogNodeVo.CATALOG);
            List<CiVo> ciVoList = catalogId2CiListMap.get(node.getId());
            if (CollectionUtils.isNotEmpty(ciVoList)) {
                node.setChildrenCount(ciVoList.size());
            }
            CiCatalogNodeVo parent = id2NodeMap.get(node.getParentId());
            if (parent != null) {
                parent.addChild(node);
            }
        }
        return rootNode.getChildren();
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalog/listtree";
    }
}
