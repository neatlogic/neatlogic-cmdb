/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.file;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CiEntityImportFileHandler extends FileTypeHandlerBase {
    @Resource
    private ImportMapper importMapper;

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        boolean isValid = false;
        if (fileVo != null && StringUtils.isNotBlank(userUuid)) {
            isValid = fileVo.getUserUuid().equals(userUuid);
        }
        return isValid;
    }

    @Override
    public String getDisplayName() {
        return "配置项上传附件";
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
        return "CIENTITYIMPORT";
    }

}
