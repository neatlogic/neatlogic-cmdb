/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


@Service
public class SelectValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getType() {
        return "select";
    }

    @Override
    public String getName() {
        return "下拉框";
    }

    @Override
    public String getIcon() {
        return "ts-list";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
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
        return false;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONObject config = attrVo.getConfig();
        JSONArray actualValueList = new JSONArray();
        Map<Long, CiEntityVo> ciEntityCachedMap = new HashMap<>();
        for (int i = 0; i < valueList.size(); i++) {
            Long value = valueList.getLong(i);
            if (config != null && config.containsKey("textKey") && value != null) {
                Long attrId = config.getLong("textKey");
                CiEntityVo ciEntityVo = ciEntityCachedMap.get(value);
                if (ciEntityVo == null) {
                    ciEntityVo = ciEntityService.getCiEntityById(attrVo.getTargetCiId(), value);
                }
                if (ciEntityVo != null) {
                    ciEntityCachedMap.put(value, ciEntityVo);
                    if (ciEntityVo.hasAttrEntityData(attrId)) {
                        AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
                        //默认只取第一个值
                        if (attrEntityVo != null) {
                            actualValueList.add(attrEntityVo.getActualValueList().get(0).toString());
                        }
                    }
                }
            }
        }
        return actualValueList;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.EQ,
                SearchExpression.NE, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 3;
    }
}
