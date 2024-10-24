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

package neatlogic.module.cmdb.attrvaluehandler.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Service
public class FileValueHandler implements IAttrValueHandler {

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getType() {
        return "file";
    }

    @Override
    public String getName() {
        return "附件";
    }

    @Override
    public String getIcon() {
        return "tsfont-attachment";
    }

    @Override
    public boolean isCanSearch() {
        return false;
    }

    @Override
    public boolean isCanSort() {
        return false;
    }

    @Override
    public boolean isCanInput() {
        return true;
    }

    @Override
    public boolean isCanImport() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isNeedTargetCi() {
        return false;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public boolean isNameAttr() {
        return false;
    }

    @Override
    public boolean isUniqueAttr() {
        return false;
    }

    @Override
    public Object transferValueListToInput(AttrVo attrVo, Object value) {
        JSONArray valueList = new JSONArray();
        if (value != null && value.toString().startsWith("[")) {
            try {
                valueList = JSON.parseArray(value.toString());
            } catch (Exception ignored) {
                valueList.add(value);
            }
        } else {
            valueList.add(value);
        }
        return valueList;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        List<Long> idList = new ArrayList<>();
        JSONArray returnList = new JSONArray();
        for (int i = 0; i < valueList.size(); i++) {
            idList.add(valueList.getLong(i));
        }
        if (CollectionUtils.isNotEmpty(idList)) {
            List<FileVo> fileList = fileMapper.getFileListByIdList(idList);
            if (CollectionUtils.isNotEmpty(fileList)) {
                for (FileVo fileVo : fileList) {
                    JSONObject fileObj = new JSONObject();
                    fileObj.put("id", fileVo.getId());
                    fileObj.put("name", fileVo.getName());
                    returnList.add(fileObj);
                }
            }
        }
        return returnList;
    }

    @Override
    public JSONArray transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                idList.add(valueList.getLong(i));
            }
            if (CollectionUtils.isNotEmpty(idList)) {
                List<FileVo> fileList = fileMapper.getFileListByIdList(idList);
                if (CollectionUtils.isNotEmpty(fileList)) {
                    for (FileVo fileVo : fileList) {
                        returnValueList.add(fileVo.getName());
                    }
                }
            }
        }
        return returnValueList;
    }


    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 12;
    }

    /**
     * 将值转换成显示的形式
     *
     * @param valueList 数据库的数据
     * @return 用于显示数据
     */
    @Override
    public void transferValueListToDisplay(AttrVo attrVo, JSONArray valueList) {
        for (int i = 0; i < valueList.size(); i++) {
            try {
                long fileId = valueList.getLongValue(i);
                FileVo fileVo = fileMapper.getFileById(fileId);
                if (fileVo != null) {
                    valueList.set(i, fileVo.getName());
                }
            } catch (Exception ignored) {
                //传进来的值不一定是id，例如视图分组后的"[空值]"
            }
        }
    }

    @Override
    public boolean valid(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                try {
                    Long fileId = Long.valueOf(value);
                    FileVo fileVo = fileMapper.getFileById(fileId);
                    if (fileVo == null) {
                        throw new AttrValueIrregularException(attrVo, value);
                    }
                } catch (NumberFormatException ex) {
                    throw new AttrValueIrregularException(attrVo, value);
                }
            }
        }
        return true;
    }
}
