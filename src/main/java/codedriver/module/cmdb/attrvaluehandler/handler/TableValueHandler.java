/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Service
public class TableValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getType() {
        return "table";
    }

    @Override
    public String getName() {
        return "表格";
    }

    @Override
    public String getIcon() {
        return "ts-tablechart";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isNeedTargetCi() {
        return true;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return true;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnList = new JSONArray();
        JSONObject config = attrVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            // JSONArray attrList = config.getJSONArray("attrList");
           /* if (CollectionUtils.isNotEmpty(attrList)) {
                for (int i = 0; i < attrList.size(); i++) {
                    JSONObject attrObj = attrList.getJSONObject(i);
                    if (attrObj.getBooleanValue("isSelected")) {
                        JSONObject headObj = new JSONObject();
                        headObj.put("key", "attr_" + attrObj.getLong("id"));
                        headObj.put("title", attrObj.getString("label"));
                        theadList.add(headObj);
                    }
                }
            }*/
            if (CollectionUtils.isNotEmpty(valueList)) {
                List<Long> ciEntityIdList = new ArrayList<>();
                for (int i = 0; i < valueList.size(); i++) {
                    try {
                        ciEntityIdList.add(valueList.getLong(i));
                    } catch (Exception ignored) {

                    }
                }
                List<CiEntityVo> ciEntityList = ciEntityService.getCiEntityByIdList(attrVo.getTargetCiId(), ciEntityIdList);
                returnList.addAll(ciEntityList);
            }
        }

        return returnList;
    }
}
