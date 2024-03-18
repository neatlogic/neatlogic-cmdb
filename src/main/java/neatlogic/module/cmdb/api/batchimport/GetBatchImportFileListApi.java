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
