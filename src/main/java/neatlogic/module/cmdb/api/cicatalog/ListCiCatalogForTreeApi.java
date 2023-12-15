package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiCatalogForTreeApi extends PrivateApiComponentBase {

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
        CiCatalogNodeVo rootNode = allNodeList.get(0);
        Map<Long, CiCatalogNodeVo> id2NodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        for (CiCatalogNodeVo node : allNodeList) {
            node.setType(CiCatalogNodeVo.CATALOG);
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
