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

package neatlogic.module.cmdb.initialdata.handler;

import neatlogic.framework.initialdata.core.IAfterInitialDataImportHandler;
import neatlogic.module.cmdb.service.ci.CiService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CmdbAfterInitialDataImportHandler implements IAfterInitialDataImportHandler {

    @Resource
    private CiService ciService;

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
        ciService.initCiTableView();
    }
}
