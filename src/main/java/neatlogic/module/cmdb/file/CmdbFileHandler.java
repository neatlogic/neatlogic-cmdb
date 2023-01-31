/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.file;

import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CmdbFileHandler extends FileTypeHandlerBase {
    @Autowired
    private ImportMapper importMapper;

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        if (fileVo != null && StringUtils.isNotBlank(userUuid)) {
            return fileVo.getUserUuid().equals(userUuid);
        }
        return false;
    }

    @Override
    public String getDisplayName() {
        return "配置管理附件";
    }

    @Override
    protected boolean myDeleteFile(FileVo fileVo, JSONObject paramObj) {
        importMapper.deleteCmdbImportFile(fileVo.getId());
        return true;
    }

    @Override
    public void afterUpload(FileVo fileVo, JSONObject jsonObj) {
    }

    @Override
    public String getName() {
        return "CMDB";
    }

}
