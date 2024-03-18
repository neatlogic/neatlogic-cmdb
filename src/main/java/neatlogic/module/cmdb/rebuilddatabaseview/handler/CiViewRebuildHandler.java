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
                viewStatusInfo.setName("cmdb_" + ciVo.getId());
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

    @Override
    public int getSort() {
        return 1;
    }
}
