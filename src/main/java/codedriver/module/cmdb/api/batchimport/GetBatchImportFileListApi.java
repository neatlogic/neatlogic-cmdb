package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import codedriver.module.cmdb.dao.mapper.batchimport.ImportMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetBatchImportFileListApi extends PrivateApiComponentBase {

    @Autowired
    private ImportMapper importMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/files/get";
    }

    @Override
    public String getName() {
        return "获取批量导入文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({@Param(name = "list",explode = FileVo.class)})
    @Description(desc = "获取批量导入文件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        List<FileVo> fileList = importMapper.getCmdbImportFileList(UserContext.get().getUserUuid());
        returnObj.put("list",fileList);
        return returnObj;
    }
}
