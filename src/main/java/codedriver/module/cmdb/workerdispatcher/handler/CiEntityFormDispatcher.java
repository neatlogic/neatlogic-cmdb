package codedriver.module.cmdb.workerdispatcher.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.workerdispatcher.core.WorkerDispatcherBase;
import codedriver.framework.process.workerdispatcher.core.WorkerDispatcherForm;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CiEntityFormDispatcher extends WorkerDispatcherBase {

    @Override
    public String getName() {
        return "配置项表单分派器";
    }

    @Override
    protected List<String> myGetWorker(ProcessTaskStepVo processTaskStepVo, JSONObject configObj) {
        List<String> resultList = new ArrayList<>();
        resultList.add(UserContext.get().getUserUuid());
        return resultList;
    }

    @Override
    public String getHelp() {
        return "根据所选表单组件，查询CMDB配置项中某一列的值作为当前步骤的处理人";
    }

    @Override
    public JSONArray getConfig() {
        JSONArray resultArray = new JSONArray();
        /** 选择表单组件 **/
        {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("type", WorkerDispatcherForm.FORM_SELECT.getValue());
            jsonObj.put("name", "form");
            jsonObj.put("label", "配置项");
            jsonObj.put("validateList", Collections.singletonList("required"));
            jsonObj.put("multiple", false);
            jsonObj.put("value", "");
            jsonObj.put("defaultValue", "");
            resultArray.add(jsonObj);
        }
        /** 选择ciEntity **/
        {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("type", WorkerDispatcherForm.TEXT.getValue());
            jsonObj.put("name", "owner");
            jsonObj.put("label", "配置项属性名");
            jsonObj.put("validateList", Collections.singletonList("required"));
            jsonObj.put("value", "");
            jsonObj.put("defaultValue", "");
            resultArray.add(jsonObj);
        }
        return resultArray;
    }

}
