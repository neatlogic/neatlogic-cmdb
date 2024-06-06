/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.file;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.file.core.FileTypeHandlerBase;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CmdbFileHandler extends FileTypeHandlerBase {
    @Resource
    private ImportMapper importMapper;

    @Override
    public boolean valid(String userUuid, FileVo fileVo, JSONObject jsonObj) {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        Long ciId = jsonObj.getLong("ciId");
        if (ciEntityId == null || ciId == null) {
            return false;
        }
        boolean isValid = false;
        if (fileVo != null && StringUtils.isNotBlank(userUuid)) {
            isValid = fileVo.getUserUuid().equals(userUuid);
        }
        if (!isValid) {
            isValid = CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciId).checkCiEntityIsInGroup(ciEntityId, GroupType.READONLY, GroupType.MAINTAIN, GroupType.AUTOEXEC).check();
        }
        return isValid;
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
