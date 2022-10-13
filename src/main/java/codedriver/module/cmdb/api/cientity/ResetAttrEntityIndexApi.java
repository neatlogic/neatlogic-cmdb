/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ResetAttrEntityIndexApi extends PrivateApiComponentBase {

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private AttrEntityMapper attrEntityMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/resetattrentityindex";
    }

    @Override
    public String getName() {
        return "重建引用属性索引";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "重建引用属性索引接口，用于优化查询性能")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CachedThreadPool.execute(new CodeDriverThread("CMDB-ATTR-INDEX-BUILDER", true) {
            @Override
            protected void execute() {
                List<AttrVo> attrList = attrMapper.getAllNeedTargetCiAttrList();
                for (AttrVo attrVo : attrList) {
                    AttrEntityVo attrEntityVo = new AttrEntityVo();
                    attrEntityVo.setAttrId(attrVo.getId());
                    attrEntityVo.setFromCiId(attrVo.getCiId());
                    attrEntityVo.setPageSize(100);
                    attrEntityVo.setCurrentPage(1);
                    List<AttrEntityVo> attrEntityList = attrEntityMapper.getAttrEntityByFromCiIdAndAttrId(attrEntityVo);
                    while (CollectionUtils.isNotEmpty(attrEntityList)) {
                        for (AttrEntityVo attrEntity : attrEntityList) {
                            ciEntityService.rebuildAttrEntityIndex(attrEntity.getAttrId(), attrEntity.getFromCiEntityId());
                        }
                        attrEntityVo.setCurrentPage(attrEntityVo.getCurrentPage() + 1);
                        attrEntityList = attrEntityMapper.getAttrEntityByFromCiIdAndAttrId(attrEntityVo);
                    }
                }
            }
        });
        return "已发起重建作业，系统会在后台完成重建";
    }

}
