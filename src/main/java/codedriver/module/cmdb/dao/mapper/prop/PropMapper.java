package codedriver.module.cmdb.dao.mapper.prop;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import codedriver.module.cmdb.dto.prop.PropVo;

public interface PropMapper {
    public int checkPropIsInUsed(Long propId);

    public int checkPropNameIsExists(PropVo propVo);

    public int checkPropLabelIsExists(PropVo propVo);

    public PropVo getPropById(Long propId);

    public List<PropVo> searchProp(PropVo propVo);

    public int searchPropCount(PropVo propVo);

    public int insertProp(PropVo propVo);

    public int updateProp(PropVo propVo);

    public int deletePropById(Long propId);
}
