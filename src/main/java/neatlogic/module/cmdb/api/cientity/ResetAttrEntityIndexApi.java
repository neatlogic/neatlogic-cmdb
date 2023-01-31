/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.asynchronization.thread.CodeDriverThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
