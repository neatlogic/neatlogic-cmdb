package neatlogic.module.cmdb.dao.mapper.cicatalog;

import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogNodeVo;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.common.dto.BasePageVo;

import java.util.List;

public interface CiCatalogMapper {

    int checkCiCatalogIsExists(Long id);

    int checkCiCatalogNameIsRepeat(CiCatalogVo ciCatalog);

    CiCatalogVo getCiCatalogById(Long id);

    int getAllCount();

    List<CiCatalogNodeVo> getCiCatalogList(BasePageVo searchVo);

    int checkCiCatalogIsInUsed(Long id);

    Long insertCiCatalog(CiCatalogVo ciCatalog);

    void deleteCiCatalog(Long id);
}
