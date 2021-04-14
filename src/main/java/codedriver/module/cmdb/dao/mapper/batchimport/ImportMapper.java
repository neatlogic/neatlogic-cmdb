/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.batchimport;

import codedriver.framework.file.dto.FileVo;
import codedriver.framework.cmdb.dto.batchimport.ImportAuditVo;

import java.util.List;

public interface ImportMapper {

    public List<FileVo> getCmdbImportFileList(String userUuid);

    public int searchImportAuditCount(ImportAuditVo vo);

    public List<ImportAuditVo> searchImportAudit(ImportAuditVo vo);

    public ImportAuditVo getImportAuditById(Long id);

    public int updateImportAudit(ImportAuditVo vo);

    public int updateImportAuditTemporary(ImportAuditVo vo);

    public int updateImportAuditStatus(ImportAuditVo vo);

    public int insertCmdbImportFile(FileVo fileVo);

    public int insertImportAudit(ImportAuditVo vo);

    public void deleteCmdbImportFile(Long fileId);

}
