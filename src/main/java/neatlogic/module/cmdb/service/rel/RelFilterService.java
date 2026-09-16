package neatlogic.module.cmdb.service.rel;

import com.alibaba.fastjson.JSONObject;

/** 模型关系候选过滤条件的保存校验。 */
public interface RelFilterService {
    /** 校验对应模型的高级搜索条件，并将空配置规范为 null。 */
    JSONObject validate(Long ciId, JSONObject filter, String parameter);
}
