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
