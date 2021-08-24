/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
        return "ts-file";
    }

    @Override
    public boolean isCanSearch() {
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
    public void transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                idList.add(valueList.getLong(i));
            }
            valueList.clear();
            if (CollectionUtils.isNotEmpty(idList)) {
                List<FileVo> fileList = fileMapper.getFileListByIdList(idList);
                if (CollectionUtils.isNotEmpty(fileList)) {
                    for (FileVo fileVo : fileList) {
                        valueList.add(fileVo.getName());
                    }
                }
            }
        }
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
        return 6;
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
}
