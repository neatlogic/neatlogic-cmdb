/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ResetRelEntityIndexApi extends PrivateApiComponentBase {

    @Resource
    private RelMapper relMapper;

    @Resource
    private RelEntityMapper relEntityMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/cientity/resetrelentityindex";
    }

    @Override
    public String getName() {
        return "重建关系索引";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "重建关系索引接口，用于优化查询性能")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CachedThreadPool.execute(new CodeDriverThread("CMDB-REL-INDEX-BUILDER", true) {
            @Override
            protected void execute() {
                List<RelVo> relList = relMapper.getAllRelList();
                for (RelVo rel : relList) {
                    RelEntityVo fromRelEntityVo = new RelEntityVo();
                    fromRelEntityVo.setRelId(rel.getId());
                    fromRelEntityVo.setFromCiId(rel.getFromCiId());
                    fromRelEntityVo.setPageSize(100);
                    fromRelEntityVo.setCurrentPage(1);
                    List<RelEntityVo> fromRelEntityList = relEntityMapper.getFromRelEntityByFromCiIdAndRelId(fromRelEntityVo);
                    while (CollectionUtils.isNotEmpty(fromRelEntityList)) {
                        for (RelEntityVo relEntity : fromRelEntityList) {
                            ciEntityService.rebuildRelEntityIndex(RelDirectionType.FROM, relEntity.getRelId(), relEntity.getFromCiEntityId());
                        }
                        fromRelEntityVo.setCurrentPage(fromRelEntityVo.getCurrentPage() + 1);
                        fromRelEntityList = relEntityMapper.getFromRelEntityByFromCiIdAndRelId(fromRelEntityVo);
                    }

                    RelEntityVo toRelEntityVo = new RelEntityVo();
                    toRelEntityVo.setRelId(rel.getId());
                    toRelEntityVo.setToCiId(rel.getToCiId());
                    toRelEntityVo.setPageSize(100);
                    toRelEntityVo.setCurrentPage(1);
                    List<RelEntityVo> toRelEntityList = relEntityMapper.getToRelEntityByToCiIdAndRelId(toRelEntityVo);
                    while (CollectionUtils.isNotEmpty(toRelEntityList)) {
                        for (RelEntityVo relEntity : toRelEntityList) {
                            ciEntityService.rebuildRelEntityIndex(RelDirectionType.TO, relEntity.getRelId(), relEntity.getToCiEntityId());
                        }
                        toRelEntityVo.setCurrentPage(toRelEntityVo.getCurrentPage() + 1);
                        toRelEntityList = relEntityMapper.getToRelEntityByToCiIdAndRelId(toRelEntityVo);
                    }
                }
            }
        });
        return "已发起重建作业，系统会在后台完成重建";
    }

}
