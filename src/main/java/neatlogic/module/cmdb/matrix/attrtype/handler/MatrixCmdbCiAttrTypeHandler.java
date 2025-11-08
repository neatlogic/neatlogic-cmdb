/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.matrix.attrtype.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.matrix.core.MatrixAttrTypeBase;
import neatlogic.framework.matrix.dto.MatrixAttributeVo;
import neatlogic.module.cmdb.constvalue.matrix.MatrixAttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
    public Set<String> getRealValueBatch(MatrixAttributeVo matrixAttributeVo, Map<String, String> valueMap) {
//        JSONObject config = matrixAttributeVo.getConfig();
//        String label = null;
//        Long ciId = null;
//        try {
//            if (MapUtils.isNotEmpty(config)) {
//                JSONObject cmdbCi = config.getJSONObject("cmdbCi");
//                label = cmdbCi.getString("label");
//                ciId = cmdbCi.getLong("ciId");
//            }
//
//            if (label == null || ciId == null) {
//                return Collections.emptySet();
//            }
//            Object component = PrivateApiComponentFactory.getInstance("neatlogic.module.cmdb.api.cientity.ListCiEntityDataForSelectApi");
//            Method method = component.getClass().getMethod("myDoService", JSONObject.class);
//            JSONObject param = new JSONObject();
//            param.put("ciId", ciId);
//            param.put("label", label);
//            param.put("defaultValue", new ArrayList<>(valueMap.keySet()));
//            Object resultObj = method.invoke(component, param);
//            if (resultObj != null) {
//                JSONObject result = JSON.parseObject(JSON.toJSONString(resultObj));
//                JSONArray dataList = result.getJSONArray("tbodyList");
//                if (CollectionUtils.isNotEmpty(dataList)) {
//                    for (Map.Entry<String, String> entry : valueMap.entrySet()) {
//                        String key = entry.getKey();
//                        for (int i = 0; i < dataList.size(); i++) {
//                            JSONObject data = dataList.getJSONObject(i);
//                            String value = data.getString("value");
//                            if (Objects.equals(key, value)) {
//                                valueMap.put(value, value);
//                            }
//                        }
//                        if (StringUtils.isBlank(entry.getValue())) {
//                            valueMap.remove(key);
//                        }
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            Throwable target = ex;
//            //如果是反射抛得异常，则需循环拆包，把真实得异常类找出来
//            while (target instanceof InvocationTargetException) {
//                target = ((InvocationTargetException) target).getTargetException();
//            }
//            String error = ex.getMessage() == null ? ExceptionUtils.getStackTrace(ex) : ex.getMessage();
//            logger.error(error);
//        }
        for (Map.Entry<String, String> entry : valueMap.entrySet()) {
            valueMap.put(entry.getKey(), entry.getKey());
        }
        return Collections.emptySet();
    }
}
