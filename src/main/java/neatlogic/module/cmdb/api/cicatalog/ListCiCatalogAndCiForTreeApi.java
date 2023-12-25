package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiAuthVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.CiAuthMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiCatalogAndCiForTreeApi extends PrivateApiComponentBase {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiAuthMapper ciAuthMapper;

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
        List<CiCatalogNodeVo> catalogList = new ArrayList<>();
        List<CiCatalogNodeVo> matchCiCatalogNodeList = new ArrayList<>();
        List<CiCatalogNodeVo> noCatalogCiNodeList = new ArrayList<>();
        List<CiCatalogNodeVo> hasCatalogCiNodeList = new ArrayList<>();
        List<CiCatalogNodeVo> allNodeList = ciCatalogService.getAllCiCatalogList();
        Map<Long, CiCatalogNodeVo> id2NodeMap = allNodeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        List<CiVo> ciList = ciMapper.getAllCi(null);
        String keyword = paramObj.getString("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase();
            List<Long> matchKeywordCiCatalogNodeIdList = new ArrayList<>();
            for (CiCatalogNodeVo node : allNodeList) {
                if (node.getName().toLowerCase().contains(keyword)) {
                    matchCiCatalogNodeList.add(node);
                    matchKeywordCiCatalogNodeIdList.add(node.getId());
                }
            }
            Iterator<CiVo> iterator = ciList.iterator();
            while (iterator.hasNext()) {
                CiVo ciVo = iterator.next();
                if (matchKeywordCiCatalogNodeIdList.contains(ciVo.getCatalogId())) {
                    continue;
                }
                if (!ciVo.getName().toLowerCase().contains(keyword) && !ciVo.getLabel().toLowerCase().contains(keyword)) {
                    iterator.remove();
                }
            }
        }
        checkCiAuth(ciList);
        for (CiVo ciVo : ciList) {
            CiCatalogNodeVo ciNode = new CiCatalogNodeVo(ciVo);
            if (ciVo.getCatalogId() == null) {
                noCatalogCiNodeList.add(ciNode);
                continue;
            }
            CiCatalogNodeVo node = id2NodeMap.get(ciVo.getCatalogId());
            if (node == null) {
                noCatalogCiNodeList.add(ciNode);
                continue;
            }
            matchCiCatalogNodeList.add(node);
            hasCatalogCiNodeList.add(ciNode);
        }

        for (CiCatalogNodeVo node : allNodeList) {
            for (CiCatalogNodeVo matchCiCatalogNode : matchCiCatalogNodeList) {
                if (node.getLft() <= matchCiCatalogNode.getLft() && node.getRht() >= matchCiCatalogNode.getRht()) {
                    catalogList.add(node);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(catalogList)) {
            for (CiCatalogNodeVo node : catalogList) {
                node.setType(CiCatalogNodeVo.CATALOG);
                CiCatalogNodeVo parent = id2NodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.addChild(node);
                }
            }
            for (CiCatalogNodeVo node : hasCatalogCiNodeList) {
                CiCatalogNodeVo parent = id2NodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.addChild(node);
                }
            }
            CiCatalogNodeVo rootNode = allNodeList.get(0);
            for (CiCatalogNodeVo node : noCatalogCiNodeList) {
                rootNode.addChild(node);
            }
            return rootNode.getChildren();
        } else {
            return noCatalogCiNodeList;
        }
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalogandci/listtree";
    }

    private void checkCiAuth(List<CiVo> ciList) {
        //如果没有管理权限则需要检查每个模型的权限
        if (!AuthActionChecker.check(CI_MODIFY.class, CIENTITY_MODIFY.class)) {
            Map<Long, List<CiAuthVo>> ciId2CiAuthListMap = new HashMap<>();
            List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
            List<CiAuthVo> ciAuthList = ciAuthMapper.getCiAuthByCiIdList(ciIdList);
            for (CiAuthVo ciAuth : ciAuthList) {
                ciId2CiAuthListMap.computeIfAbsent(ciAuth.getCiId(), key -> new ArrayList<>()).add(ciAuth);
            }
            Iterator<CiVo> iterator = ciList.iterator();
            while (iterator.hasNext()) {
                CiVo ciVo = iterator.next();
                List<CiAuthVo> ciAuths = ciId2CiAuthListMap.get(ciVo.getId());
                if (CollectionUtils.isNotEmpty(ciAuths)) {
                    if (!CiAuthChecker.hasPrivilege(ciAuths, CiAuthType.CIMANAGE, CiAuthType.CIENTITYUPDATE, CiAuthType.CIENTITYDELETE, CiAuthType.TRANSACTIONMANAGE, CiAuthType.CIENTITYQUERY)) {
                        if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                            iterator.remove();
                        }
                    }
                } else {
                    if (!CiAuthChecker.isCiInGroup(ciVo.getId(), GroupType.READONLY, GroupType.MAINTAIN)) {
                        iterator.remove();
                    }
                }
            }
        }
    }
}
