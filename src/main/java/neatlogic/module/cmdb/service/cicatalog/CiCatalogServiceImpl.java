package neatlogic.module.cmdb.service.cicatalog;

import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.module.cmdb.dao.mapper.cicatalog.CiCatalogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CiCatalogServiceImpl implements CiCatalogService {

    @Resource
    private CiCatalogMapper ciCatalogMapper;

    @Override
    public int checkCiCatalogIsExists(Long id) {
        return ciCatalogMapper.checkCiCatalogIsExists(id);
    }

    @Override
    public int checkCiCatalogNameIsRepeat(CiCatalogVo ciCatalog) {
        return ciCatalogMapper.checkCiCatalogNameIsRepeat(ciCatalog);
    }

    @Override
    public CiCatalogVo getCiCatalog(Long id) {
        return ciCatalogMapper.getCiCatalogById(id);
    }

    @Override
    public List<CiCatalogNodeVo> getAllCiCatalogList() {
        int rowNum = ciCatalogMapper.getAllCount();
        if (rowNum == 0) {
            return new ArrayList<>();
        }
        CiCatalogNodeVo rootNode = new CiCatalogNodeVo();
        rootNode.setId(CiCatalogVo.ROOT_ID);
        rootNode.setName(CiCatalogVo.ROOT_NAME);
        rootNode.setParentId(CiCatalogVo.ROOT_PARENTID);
        rootNode.setLft(1);
        rootNode.setRht((rowNum + 1) * 2);
        List<CiCatalogNodeVo> allNodeList = new ArrayList<>();
        allNodeList.add(rootNode);
        BasePageVo searchVo = new BasePageVo();
        searchVo.setPageSize(100);
        searchVo.setRowNum(rowNum);
        int pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<CiCatalogNodeVo> serviceNodeList = ciCatalogMapper.getCiCatalogList(searchVo);
            allNodeList.addAll(serviceNodeList);
        }
        return allNodeList;
    }

    @Override
    public int checkCiCatalogIsInUsed(Long id) {
        return ciCatalogMapper.checkCiCatalogIsInUsed(id);
    }

    @Override
    public Long saveCiCatalog(CiCatalogVo ciCatalog) {
        CiCatalogVo oldCiCatalog = ciCatalogMapper.getCiCatalogById(ciCatalog.getId());
        if (oldCiCatalog == null) {
            int lft = LRCodeManager.beforeAddTreeNode("cmdb_ci_catalog", "id", "parent_id", ciCatalog.getParentId());
            ciCatalog.setLft(lft);
            ciCatalog.setRht(lft + 1);
        } else {
            if (Objects.equals(oldCiCatalog.getName(), ciCatalog.getName())) {
                return ciCatalog.getId();
            }
        }
        ciCatalogMapper.insertCiCatalog(ciCatalog);
        return ciCatalog.getId();
    }

    @Override
    public void deleteCiCatalog(Long id) {
        LRCodeManager.beforeDeleteTreeNode("cmdb_ci_catalog", "id", "parent_id", id);
        ciCatalogMapper.deleteCiCatalog(id);
    }
}
