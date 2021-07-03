/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.batchimport;

import codedriver.framework.file.dto.FileVo;
import codedriver.framework.cmdb.dto.batchimport.ImportAuditVo;

import java.util.List;

public interface ImportMapper {

    List<FileVo> getCmdbImportFileList(String userUuid);

    int searchImportAuditCount(ImportAuditVo vo);

    List<ImportAuditVo> searchImportAudit(ImportAuditVo vo);

    ImportAuditVo getImportAuditById(Long id);

    int updateImportAudit(ImportAuditVo vo);

    int updateImportAuditTemporary(ImportAuditVo vo);

    int updateImportAuditStatus(ImportAuditVo vo);

    int insertCmdbImportFile(FileVo fileVo);

    int insertImportAudit(ImportAuditVo vo);

    void deleteCmdbImportFile(Long fileId);

}
