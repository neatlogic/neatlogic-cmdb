package neatlogic.module.cmdb.importexport.handler;

import neatlogic.framework.cmdb.dto.ci.CiTypeVo;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.cmdb.exception.ci.CiTypeNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.cmdb.dao.mapper.ci.CiTypeMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CiTypeImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private CiTypeMapper ciTypeMapper;

    @Override
    public ImportExportHandlerType getType() {
        return CmdbImportExportHandlerType.CI_TYPE;
    }

    @Override
    public boolean checkImportAuth(ImportExportVo importExportVo) {
        return true;
    }

    @Override
    public boolean checkExportAuth(Object primaryKey) {
        return true;
    }

    @Override
    public boolean checkIsExists(ImportExportBaseInfoVo importExportBaseInfoVo) {
        return ciTypeMapper.getCiTypeByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        CiTypeVo ciType = ciTypeMapper.getCiTypeByName(importExportVo.getName());
        if (ciType == null) {
            throw new CiTypeNotFoundException(importExportVo.getName());
        }
        return ciType.getId();
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        CiTypeVo ciType = importExportVo.getData().toJavaObject(CiTypeVo.class);
        CiTypeVo oldCiType = ciTypeMapper.getCiTypeByName(importExportVo.getName());
        if (oldCiType != null) {
            ciType.setId(oldCiType.getId());
            ciTypeMapper.updateCiType(ciType);
        } else {
            if (ciTypeMapper.getCiTypeById(ciType.getId()) != null) {
                ciType.setId(null);
            }
            Integer maxsort = ciTypeMapper.getMaxSort();
            if (maxsort == null) {
                maxsort = 1;
            } else {
                maxsort += 1;
            }
            ciType.setSort(maxsort);
            ciTypeMapper.insertCiType(ciType);
        }
        return ciType.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        CiTypeVo ciType = ciTypeMapper.getCiTypeById(id);
        if (ciType == null) {
            throw new CiTypeNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, ciType.getName());
        importExportVo.setDataWithObject(ciType);
        return importExportVo;
    }
}
