package neatlogic.module.cmdb.importexport.handler;

import neatlogic.framework.cmdb.dao.mapper.validator.ValidatorMapper;
import neatlogic.framework.cmdb.dto.validator.ValidatorVo;
import neatlogic.framework.cmdb.enums.CmdbImportExportHandlerType;
import neatlogic.framework.cmdb.exception.validator.ValidatorNotFoundException;
import neatlogic.framework.importexport.core.ImportExportHandlerBase;
import neatlogic.framework.importexport.core.ImportExportHandlerType;
import neatlogic.framework.importexport.dto.ImportExportBaseInfoVo;
import neatlogic.framework.importexport.dto.ImportExportPrimaryChangeVo;
import neatlogic.framework.importexport.dto.ImportExportVo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Component
public class CiValidatorImportExportHandler extends ImportExportHandlerBase {

    @Resource
    private ValidatorMapper validatorMapper;

    @Override
    public ImportExportHandlerType getType() {
        return CmdbImportExportHandlerType.CI_VALIDATOR;
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
        return validatorMapper.getValidatorByName(importExportBaseInfoVo.getName()) != null;
    }

    @Override
    public Object getPrimaryByName(ImportExportVo importExportVo) {
        ValidatorVo validator = validatorMapper.getValidatorByName(importExportVo.getName());
        if (validator == null) {
            throw new ValidatorNotFoundException(importExportVo.getName());
        }
        return validator.getId();
    }

    @Override
    public Object importData(ImportExportVo importExportVo, List<ImportExportPrimaryChangeVo> primaryChangeList) {
        ValidatorVo validator = importExportVo.getData().toJavaObject(ValidatorVo.class);
        ValidatorVo oldValidator = validatorMapper.getValidatorByName(importExportVo.getName());
        if (oldValidator != null) {
            validator.setId(oldValidator.getId());
            validatorMapper.updateValidator(validator);
        } else {
            if (validatorMapper.getValidatorById(validator.getId()) != null) {
                validator.setId(null);
            }
            validatorMapper.insertValidator(validator);
        }
        return validator.getId();
    }

    @Override
    protected ImportExportVo myExportData(Object primaryKey, List<ImportExportBaseInfoVo> dependencyList, ZipOutputStream zipOutputStream) {
        Long id = (Long) primaryKey;
        ValidatorVo validator = validatorMapper.getValidatorById(id);
        if (validator == null) {
            throw new ValidatorNotFoundException(id);
        }
        ImportExportVo importExportVo = new ImportExportVo(this.getType().getValue(), primaryKey, validator.getName());
        importExportVo.setDataWithObject(validator);
        return importExportVo;
    }
}
