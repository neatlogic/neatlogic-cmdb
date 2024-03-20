/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.matrix.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.matrix.core.MatrixDataSourceHandlerBase;
import neatlogic.framework.matrix.dto.MatrixAttributeVo;
import neatlogic.framework.matrix.dto.MatrixDataVo;
import neatlogic.framework.matrix.dto.MatrixVo;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.matrix.constvalue.MatrixType;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import neatlogic.module.cmdb.workerdispatcher.exception.CmdbDispatcherDispatchFailedException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CmdbCustomViewDataSourceHandler extends MatrixDataSourceHandlerBase {

    private final static Logger logger = LoggerFactory.getLogger(CmdbCustomViewDataSourceHandler.class);

    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CustomViewDataService customViewDataService;

    @Override
    public String getHandler() {
        return MatrixType.CMDBCUSTOMVIEW.getValue();
    }

    @Override
    protected boolean mySaveMatrix(MatrixVo matrixVo) throws Exception {
        Long customViewId = matrixVo.getCustomViewId();
        if (customViewId != null) {
            throw new ParamNotExistsException("customViewId");
        }
        CustomViewVo customView = customViewMapper.getCustomViewById(customViewId);
        if (customView == null) {
            throw new CmdbDispatcherDispatchFailedException(customViewId);
        }
        JSONObject config = matrixVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            throw new ParamNotExistsException("config");
        }
        JSONArray showAttributeLabelArray = config.getJSONArray("showAttributeLabelList");
        if (CollectionUtils.isEmpty(showAttributeLabelArray)) {
            throw new ParamNotExistsException("config.showAttributeLabelList");
        }
        return false;
    }

    @Override
    protected void myGetMatrix(MatrixVo matrixVo) {

    }

    @Override
    protected void myDeleteMatrix(String uuid) {

    }

    @Override
    protected void myCopyMatrix(String sourceUuid, MatrixVo matrixVo) {

    }

    @Override
    protected JSONObject myImportMatrix(MatrixVo matrixVo, MultipartFile multipartFile) throws IOException {
        return null;
    }

    @Override
    protected MatrixVo myExportMatrix(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected void myImportMatrix(MatrixVo matrixVo) {

    }

    @Override
    protected void mySaveAttributeList(String matrixUuid, List<MatrixAttributeVo> matrixAttributeList) {

    }

    @Override
    protected List<MatrixAttributeVo> myGetAttributeList(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected JSONObject myExportAttribute(MatrixVo matrixVo) {
        return null;
    }

    @Override
    protected JSONObject myTableDataSearch(MatrixDataVo dataVo) {
        return null;
    }

    @Override
    protected JSONObject mySaveTableRowData(String matrixUuid, JSONObject rowData) {
        return null;
    }

    @Override
    protected Map<String, String> myGetTableRowData(MatrixDataVo matrixDataVo) {
        return null;
    }

    @Override
    protected void myDeleteTableRowData(String matrixUuid, List<String> uuidList) {

    }
}
