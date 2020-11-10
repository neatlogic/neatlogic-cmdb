package codedriver.module.cmdb.dao.mapper.batchimport;

import codedriver.framework.file.dto.FileVo;

import java.util.List;

public interface ImportMapper {

    public List<FileVo> getCmdbImportFileList(String userUuid);

    public int insertCmdbImportFile(FileVo fileVo);

    public void deleteCmdbImportFile(Long fileId);

}
