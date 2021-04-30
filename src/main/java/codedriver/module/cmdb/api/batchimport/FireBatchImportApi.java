/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.ExcelFormatIllegalException;
import codedriver.framework.exception.file.ExcelNameIllegalException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_BATCH_IMPORT;
import codedriver.module.cmdb.dao.mapper.batchimport.ImportMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.plugin.BatchImportHandler;
import com.alibaba.fastjson.JSONObject;
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
        if (fileList.size() > 0) {
            for (FileVo fileVo : fileList) {
                if (!fileVo.getName().endsWith(".xls") && !fileVo.getName().endsWith(".xlsx")) {
                    throw new ExcelFormatIllegalException(".xls或.xlsx");
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
