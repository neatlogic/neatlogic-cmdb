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

package neatlogic.module.cmdb.api.batchimport;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetBatchImportFileListApi extends PrivateApiComponentBase {

    @Resource
    private ImportMapper importMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/files/get";
    }

    @Override
    public String getName() {
        return "nmcab.getbatchimportfilelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(name = "list", explode = FileVo.class)})
    @Description(desc = "nmcab.getbatchimportfilelistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        List<FileVo> fileList = importMapper.getCmdbImportFileList(UserContext.get().getUserUuid());
        returnObj.put("list", fileList);
        return returnObj;
    }
}
