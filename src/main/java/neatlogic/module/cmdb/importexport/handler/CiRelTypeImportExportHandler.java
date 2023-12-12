package neatlogic.module.cmdb.importexport.handler;

import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.cmdb.exception.reltype.RelTypeNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import neatlogic.module.cmdb.dao.mapper.ci.RelTypeMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CiRelTypeImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private RelTypeMapper relTypeMapper;

    @Override
    public ImportExportHandlerType getType() {
        return CmdbImportExportHandlerType.CI_REL_TYPE;
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
        return relTypeMapper.getRelTypeByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        RelTypeVo relType = relTypeMapper.getRelTypeByName(importExportVo.getName());
        if (relType == null) {
            throw new RelTypeNotFoundException(importExportVo.getName());
        }
        return relType.getId();
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        RelTypeVo relType = importExportVo.getData().toJavaObject(RelTypeVo.class);
        RelTypeVo oldRelType = relTypeMapper.getRelTypeByName(importExportVo.getName());
        if (oldRelType != null) {
            relType.setId(oldRelType.getId());
            relTypeMapper.updateRelType(relType);
        } else {
            if (relTypeMapper.getRelTypeById(relType.getId()) != null) {
                relType.setId(null);
            }
            relTypeMapper.insertRelType(relType);
        }
        return relType.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        RelTypeVo relType = relTypeMapper.getRelTypeById(id);
        if (relType == null) {
            throw new RelTypeNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, relType.getName());
        importExportVo.setDataWithObject(relType);
        return importExportVo;
    }
}
