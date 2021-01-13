package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import codedriver.module.cmdb.plugin.BatchImportHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@AuthAction(action = CIENTITY_BATCH_IMPORT.class)
@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class StopBatchImportApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/cmdb/import/stop";
    }

    @Override
    public String getName() {
        return "停止批量导入";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true,desc = "导入记录ID")})
    @Output({})
    @Description(desc = "停止批量导入")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        BatchImportHandler.stopImportById(id);
        return null;
    }
}
