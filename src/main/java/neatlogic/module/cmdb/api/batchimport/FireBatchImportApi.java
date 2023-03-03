/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.batchimport;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.file.ExcelFormatIllegalException;
import neatlogic.framework.exception.file.ExcelNameIllegalException;
import neatlogic.framework.exception.file.FileNotUploadException;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import neatlogic.module.cmdb.plugin.BatchImportHandler;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class FireBatchImportApi extends PrivateApiComponentBase {

    @Autowired
    private ImportMapper importMapper;


    @Override
    public String getToken() {
        return "/cmdb/import/fire";
    }

    @Override
    public String getName() {
        return "发起批量导入";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "action", type = ApiParamType.ENUM, rule = "append,update,all", isRequired = true, desc = "append:只添加;update:只更新;all:添加&更新"),
            @Param(name = "editMode", type = ApiParamType.ENUM, rule = "global,partial", isRequired = true, desc = "编辑模式")
    })
    @Output({})
    @Description(desc = "发起批量导入")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String userUuid = UserContext.get().getUserUuid();
        String action = paramObj.getString("action");
        String editMode = paramObj.getString("editMode");
        List<FileVo> fileList = importMapper.getCmdbImportFileList(userUuid);
        if (CollectionUtils.isNotEmpty(fileList)) {
            for (FileVo fileVo : fileList) {
                if (!fileVo.getName().endsWith(".xls") && !fileVo.getName().endsWith(".xlsx")) {
                    throw new ExcelFormatIllegalException();
                }
                if (fileVo.getName().contains("_")) {
                    Long ciId = null;
                    try {
                        ciId = Long.parseLong(fileVo.getName().split("_")[0].trim());
                    } catch (Exception ignored) {
                    }

                    if (ciId == null) {
                        throw new ExcelNameIllegalException("“ciId_名称”，e.g.:29_应用子系统");
                    }
                    CachedThreadPool.execute(new BatchImportHandler.Importer(ciId, action, editMode, fileVo, userUuid));
                    importMapper.deleteCmdbImportFile(fileVo.getId());
                } else {
                    throw new ExcelNameIllegalException("“ciId_名称”，e.g.:29_应用子系统");
                }
            }
        } else {
            throw new FileNotUploadException();
        }
        return null;
    }
}
