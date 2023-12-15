package neatlogic.module.cmdb.service.cicatalog;

import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;

import java.util.List;

public interface CiCatalogService {

    int checkCiCatalogIsExists(Long id);

    int checkCiCatalogNameIsRepeat(CiCatalogVo ciCatalog);

    CiCatalogVo getCiCatalog(Long id);

    List<CiCatalogNodeVo> getAllCiCatalogList();

    int checkCiCatalogIsInUsed(Long id);

    Long saveCiCatalog(CiCatalogVo ciCatalog);

    void deleteCiCatalog(Long id);
}
