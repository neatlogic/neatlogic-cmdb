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
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;

import javax.annotation.Resource;
import java.util.List;

//@Service
@Deprecated
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
        return "nmcac.resetattrentityindexapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "nmcac.resetattrentityindexapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CachedThreadPool.execute(new NeatLogicThread("CMDB-ATTR-INDEX-BUILDER", true) {
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
