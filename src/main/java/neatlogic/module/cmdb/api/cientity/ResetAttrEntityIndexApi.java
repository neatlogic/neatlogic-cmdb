/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
