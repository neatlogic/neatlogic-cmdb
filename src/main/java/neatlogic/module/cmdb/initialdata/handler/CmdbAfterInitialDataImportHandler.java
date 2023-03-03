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

package neatlogic.module.cmdb.initialdata.handler;

import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.initialdata.core.IAfterInitialDataImportHandler;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CmdbAfterInitialDataImportHandler implements IAfterInitialDataImportHandler {
    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiService ciService;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getModuleId() {
        return "cmdb";
    }

    @Override
    public String getDescription() {
        return "根据模型生成数据表和视图";
    }

    @Override
    public void execute() {
        List<CiVo> ciList = ciMapper.searchCi(new CiVo());
        for (CiVo ciVo : ciList) {
            if (ciVo.getIsVirtual().equals(0)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                ciVo.setAttrList(attrList);
                ciSchemaMapper.initCiTable(ciVo);
            } else {
                //创建视图
                String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                if (StringUtils.isNotBlank(viewXml)) {
                    ciVo.setViewXml(viewXml);
                    ciService.buildCiView(ciVo);
                }
            }
        }

    }
}
