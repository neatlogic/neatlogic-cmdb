/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class TestApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;

    @Override
    public String getName() {
        return "测试缓存";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "测试二级缓存")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        for (int j = 0; j < 3; j++) {
            int finalJ = j;
            CodeDriverThread t = new CodeDriverThread() {
                @Override
                protected void execute() {
                    List<CiVo> ciList = ciMapper.getAllCi(null);
                    System.out.println(finalJ + " size:" + ciList.size());
                    for (int i = ciList.size() - 1; i >= 0; i--) {
                        if (finalJ % (i + 1) == 0) {
                            ciList.remove(i);
                        }
                    }
                    System.out.println(finalJ + " size:" + ciList.size());
                }
            };
            CachedThreadPool.execute(t);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/testcache";
    }
}
