package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.plugin.BatchImportHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@AuthAction(name = "CIENTITY_BATCH_IMPORT")
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetImportStatusApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/import/status/get";
    }

    @Override
    public String getName() {
        return "获取导入状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "日志ID")})
    @Output({})
    @Description(desc = "获取导入状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        return BatchImportHandler.getStatusById(id);
    }
}
