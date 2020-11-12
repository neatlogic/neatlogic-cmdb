package codedriver.module.cmdb.dao.mapper.batchimport;

import codedriver.framework.file.dto.FileVo;
import codedriver.module.cmdb.dto.batchimport.ImportAuditVo;

import java.util.List;

public interface ImportMapper {

    public List<FileVo> getCmdbImportFileList(String userUuid);

    public int searchImportAuditCount(ImportAuditVo vo);

    public List<ImportAuditVo> searchImportAudit(ImportAuditVo vo);

    public int updateImportAudit(ImportAuditVo vo);

    public int updateImportAuditTemporary(ImportAuditVo vo);

    public int insertCmdbImportFile(FileVo fileVo);

    public int insertImportAudit(ImportAuditVo vo);

    public void deleteCmdbImportFile(Long fileId);

}
