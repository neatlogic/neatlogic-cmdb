package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.CommonThreadPool;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.ExcelFormatIllegalException;
import codedriver.framework.exception.file.ExcelNameIllegalException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.batchimport.ImportMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.plugin.BatchImportHandler;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class FireBatchImportFileApi extends PrivateApiComponentBase {

    @Autowired
    private ImportMapper importMapper;

    @Autowired
    private CiMapper ciMapper;

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
            @Param(name = "checkProp", type = ApiParamType.ENUM, rule = "0,1", desc = "是否严格校验属性"),
            @Param(name = "action", type = ApiParamType.ENUM, rule = "append,update,all", isRequired = true, desc = "append:只添加;update:只更新;all:添加&更新"),
            @Param(name = "editMode", type = ApiParamType.ENUM, rule = "0,1",isRequired = true, desc = "是否全局更新")
    })
    @Output({})
    @Description(desc = "发起批量导入")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String userUuid = UserContext.get().getUserUuid();
        Integer checkProp = paramObj.getInteger("checkProp");
        String action = paramObj.getString("action");
        int editMode = paramObj.getIntValue("editMode");
        boolean isCheckProp = false;
        if(checkProp.intValue() == 1){
            isCheckProp = true;
        }
        List<FileVo> fileList = importMapper.getCmdbImportFileList(userUuid);
        if (fileList.size() > 0) {
            for (FileVo fileVo : fileList) {
                if (!fileVo.getName().endsWith(".xls") && !fileVo.getName().endsWith(".xlsx")) {
                    throw new ExcelFormatIllegalException(".xls或.xlsx");
                }
                if (fileVo.getName().indexOf("_") > -1) {
                    Long ciId = null;

                    ciId = Long.parseLong(fileVo.getName().split("_")[0].trim());

                    if (ciId == null) {
                        throw new ExcelNameIllegalException("“ciId_名称”，e.g.:29_应用子系统");
                    }
                    if(ciMapper.getCiById(ciId) == null){
                        throw new CiNotFoundException(ciId);
                    }
                    CommonThreadPool.execute(new BatchImportHandler.Importer(ciId, action,editMode, fileVo, userUuid, isCheckProp));
                }else {
                    throw new ExcelNameIllegalException("“ciId_名称”，e.g.:29_应用子系统");
                }
            }
        }else{
            throw new FileNotUploadException();
        }
        return null;
    }
}
