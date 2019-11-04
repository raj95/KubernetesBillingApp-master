package kubernetes.billing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ResourceUsageVO {

    private Map<String, BigDecimal> avgusageData;

    public Object[][] getTenantresourceallocation() {
        return tenantresourceallocation;
    }

    public void setTenantresourceallocation(Object[][] tenantresourceallocation) {
        this.tenantresourceallocation = tenantresourceallocation;
    }

    private Object[][] tenantresourceallocation;

    public Map<String, BigDecimal> getAvgusageData() {
        return avgusageData;
    }

    public void setAvgusageData(Map<String, BigDecimal> avgusageData) {
        this.avgusageData = avgusageData;
    }
}
