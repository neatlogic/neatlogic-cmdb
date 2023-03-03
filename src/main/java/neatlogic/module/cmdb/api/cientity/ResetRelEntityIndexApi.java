/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
        CachedThreadPool.execute(new NeatLogicThread("CMDB-REL-INDEX-BUILDER", true) {
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
