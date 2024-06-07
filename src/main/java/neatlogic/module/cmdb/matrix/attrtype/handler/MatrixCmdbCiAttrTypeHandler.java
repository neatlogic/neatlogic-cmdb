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

package neatlogic.module.cmdb.matrix.attrtype.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.matrix.core.MatrixAttrTypeBase;
import neatlogic.framework.matrix.dto.MatrixAttributeVo;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentFactory;
import neatlogic.module.cmdb.constvalue.matrix.MatrixAttributeType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Service
public class MatrixCmdbCiAttrTypeHandler extends MatrixAttrTypeBase {
    private final Logger logger = LoggerFactory.getLogger(MatrixCmdbCiAttrTypeHandler.class);

    @Override
    public String getHandler() {
        return MatrixAttributeType.CMDBCI.getValue();
    }

    @Override
    public void getTextByValue(MatrixAttributeVo matrixAttribute, Object valueObj, JSONObject resultObj) {
        String value = valueObj.toString();
        resultObj.put("text", value);
    }

    @Override
    public void getRealValueBatch(MatrixAttributeVo matrixAttributeVo, Map<String, String> valueMap) {
        JSONObject config = matrixAttributeVo.getConfig();
        String label = null;
        Long ciId = null;
        try {
            if (MapUtils.isNotEmpty(config)) {
                JSONObject cmdbCi = config.getJSONObject("cmdbCi");
                label = cmdbCi.getString("label");
                ciId = cmdbCi.getLong("ciId");
            }

            if (label == null || ciId == null) {
                return;
            }
            Object component = PrivateApiComponentFactory.getInstance("neatlogic.module.cmdb.api.cientity.ListCiEntityDataForSelectApi");
            Method method = component.getClass().getMethod("myDoService", JSONObject.class);
            JSONObject param = new JSONObject();
            param.put("ciId", ciId);
            param.put("label", label);
            param.put("defaultValue", new ArrayList<>(valueMap.keySet()));
            Object resultObj = method.invoke(component, param);
            if (resultObj != null) {
                JSONObject result = JSON.parseObject(JSON.toJSONString(resultObj));
                JSONArray dataList = result.getJSONArray("tbodyList");
                if (CollectionUtils.isNotEmpty(dataList)) {
                    for (Map.Entry<String, String> entry : valueMap.entrySet()) {
                        String key = entry.getKey();
                        for (int i = 0; i < dataList.size(); i++) {
                            JSONObject data = dataList.getJSONObject(i);
                            String value = data.getString("value");
                            if (Objects.equals(key, value)) {
                                valueMap.put(value, value);
                            }
                        }
                        if (StringUtils.isBlank(entry.getValue())) {
                            valueMap.remove(key);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Throwable target = ex;
            //如果是反射抛得异常，则需循环拆包，把真实得异常类找出来
            while (target instanceof InvocationTargetException) {
                target = ((InvocationTargetException) target).getTargetException();
            }
            String error = ex.getMessage() == null ? ExceptionUtils.getStackTrace(ex) : ex.getMessage();
            logger.error(error);
        }
    }
}
