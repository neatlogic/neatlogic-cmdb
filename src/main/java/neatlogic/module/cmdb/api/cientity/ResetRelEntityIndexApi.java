/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;

import javax.annotation.Resource;
import java.util.List;

//@Service
@Deprecated
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
        return "nmcac.resetrelentityindexapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "nmcac.resetrelentityindexapi.description.desc")
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
