package neatlogic.module.cmdb.workerdispatcher.exception;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionFilterVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.process.exception.processtask.ProcessTaskException;

import java.util.List;
import java.util.Map;

public class CmdbDispatcherDispatchFailedException extends ProcessTaskException {

    public CmdbDispatcherDispatchFailedException(Long id) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_a", id);
    }

    public CmdbDispatcherDispatchFailedException(CiEntityVo ciEntity) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_b", JSONObject.toJSONString(ciEntity));
    }

    public CmdbDispatcherDispatchFailedException(Map<String, Object> data) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_b", JSONObject.toJSONString(data));
    }

    public CmdbDispatcherDispatchFailedException(CiVo ci, List<AttrFilterVo> attrFilterList) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_c", ci.getLabel(), ci.getName(), JSONObject.toJSONString(attrFilterList));
    }

    public CmdbDispatcherDispatchFailedException(CiVo ci, List<AttrFilterVo> attrFilterList, int size) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_d", ci.getLabel(), ci.getName(), JSONObject.toJSONString(attrFilterList), size);
    }

    public CmdbDispatcherDispatchFailedException(CustomViewVo customView, List<CustomViewConditionFilterVo> attrFilterList) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_e", customView.getName(), JSONObject.toJSONString(attrFilterList));
    }

    public CmdbDispatcherDispatchFailedException(CustomViewVo customView, List<CustomViewConditionFilterVo> attrFilterList, int size) {
        super("nmcwe.cmdbdispatcherdispatchfailedexception.cmdbdispatcherdispatchfailedexception_f", customView.getName(), JSONObject.toJSONString(attrFilterList), size);
    }
}
