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

package neatlogic.module.cmdb.rebuilddatabaseview.handler;

import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.rebuilddatabaseview.core.IRebuildDataBaseView;
import neatlogic.framework.rebuilddatabaseview.core.ViewStatusInfo;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CiViewRebuildHandler implements IRebuildDataBaseView {

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiService ciService;

    @Override
    public String getDescription() {
        return "重建配置项中虚拟模型视图";
    }

    @Override
    public List<ViewStatusInfo> execute() {
        List<ViewStatusInfo> resultList = new ArrayList<>();
        int rowNum = ciMapper.getVirtualCiCount();
        if (rowNum == 0) {
            return resultList;
        }
        BasePageVo searchVo = new BasePageVo();
        searchVo.setRowNum(rowNum);
        searchVo.setPageSize(100);
        int pageCount = searchVo.getPageCount();
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            searchVo.setCurrentPage(currentPage);
            List<CiVo> ciList = ciMapper.getVirtualCiList(searchVo);
            for (CiVo ciVo : ciList) {
                ViewStatusInfo viewStatusInfo = new ViewStatusInfo();
                viewStatusInfo.setViewName("cmdb_" + ciVo.getId());
                viewStatusInfo.setLabel(ciVo.getLabel() + "(" + ciVo.getName() + ")");
                String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                ciVo.setViewXml(viewXml);
                try {
                    ciService.buildCiView(ciVo);
                    viewStatusInfo.setStatus(ViewStatusInfo.Status.SUCCESS.toString());
                } catch (Exception e) {
                    viewStatusInfo.setStatus(ViewStatusInfo.Status.FAILURE.toString());
                    viewStatusInfo.setError(e.getMessage());
                }
                resultList.add(viewStatusInfo);
            }
        }
        return resultList;
    }
}
