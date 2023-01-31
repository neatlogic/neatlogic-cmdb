/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.dao.mapper.batchimport;

import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.cmdb.dto.batchimport.ImportAuditVo;

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

    void deleteAuditByDayBefore(int dayBefore);

}
