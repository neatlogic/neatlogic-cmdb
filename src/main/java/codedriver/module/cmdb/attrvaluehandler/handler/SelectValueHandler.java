/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrvaluehandler.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class SelectValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getType() {
        return "select";
    }

    @Override
    public String getActualValue(JSONObject config, String value) {
        if (config != null && config.containsKey("textKey") && StringUtils.isNotBlank(value) && StringUtils.isNumeric(value)) {
            Long ciEntityId = Long.parseLong(value);
            Long attrId = config.getLong("textKey");
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(ciEntityId);
            if (ciEntityVo != null) {
                if (ciEntityVo.hasAttrEntityData(attrId)) {
                    JSONObject attrObj = ciEntityVo.getAttrEntityDataByAttrId(attrId);
                    //默认只取第一个值
                    return attrObj.getJSONArray("actualValueList").getString(0);
                }
            }
        }
        return null;
    }
}
