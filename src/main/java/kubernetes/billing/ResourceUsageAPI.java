package kubernetes.billing;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1ResourceQuota;
import io.kubernetes.client.models.V1ResourceQuotaList;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class ResourceUsageAPI {


    @Autowired
    ConfigProperties configProp;

    public Map<String, BigDecimal> getAvailableComputeAndMemoryResources()
    {
        Map resourceMap = new HashMap<String, Quantity>();
        try
        {

            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();

           V1NodeList nodelist  = api.listNode(false,"true", null,
                null,null,null, null, null, null);

           BigDecimal totalCpu = new BigDecimal(0);
           BigDecimal totalMemory = new BigDecimal(0);
           for(V1Node node: nodelist.getItems())
           {

               Quantity cpuQuant = node.getStatus().getCapacity().get("cpu");
               totalCpu = cpuQuant.getNumber().add(totalCpu);
               Quantity memoryQuant = node.getStatus().getCapacity().get("memory");
               totalMemory =  memoryQuant.getNumber().add(totalMemory);

           }
           System.out.println("TOTAL CPU IS-->" + totalCpu);
            System.out.println("TOTAL MEMORY IS-->" + totalMemory);
            resourceMap.put("cpu", totalCpu);
            resourceMap.put("memory", totalMemory);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return resourceMap;
    }

    public Map<String,  HashMap<String, BigDecimal>> getTenantsComputeAndMemoryUsage()
    {
        Map<String,  HashMap<String, BigDecimal>> tenantresourceMap = new HashMap<String,  HashMap<String, BigDecimal>>();
        try
        {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            CoreV1Api api = new CoreV1Api();

            V1ResourceQuotaList resourceQuotaList =  api.listResourceQuotaForAllNamespaces(null,null,null,null,
                    null,"false", null, null, null);

            for(V1ResourceQuota vq: resourceQuotaList.getItems())
            {
                HashMap<String, BigDecimal> tenantresourceusageMap = new HashMap<String, BigDecimal>();
                for (Map.Entry<String,Quantity> entry : vq.getSpec().getHard().entrySet())
                {
                    if(entry.getKey().equals("limits.cpu"))
                    {
                        System.out.println("TENANT CPU IS-->" + vq.getMetadata().getNamespace() + "-->" +  entry.getValue().getNumber());
                        tenantresourceusageMap.put("cpu", entry.getValue().getNumber());
                    }
                    else if(entry.getKey().equals("limits.memory"))
                    {
                        System.out.println("TENANT MEMORY IS-->" + vq.getMetadata().getNamespace() + "-->" + entry.getValue().getNumber());
                        tenantresourceusageMap.put("memory", entry.getValue().getNumber());
                    }


                }
                tenantresourceMap.put(vq.getMetadata().getNamespace(), tenantresourceusageMap);


            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return tenantresourceMap;


    }

    public  ResourceUsageVO getTenantUsageInPercentage()
    {
        ResourceUsageVO usageVo = new ResourceUsageVO();

        Map<String, BigDecimal> availableComputeMemResource = getAvailableComputeAndMemoryResources();
        Map<String, HashMap<String, BigDecimal>> tenantComputeMemResource = getTenantsComputeAndMemoryUsage();
        BigDecimal totalcpu = availableComputeMemResource.get("cpu");
        BigDecimal totalmemory = availableComputeMemResource.get("memory");

        Map<String, BigDecimal> usageData = new HashMap<String, BigDecimal>();

        String[] namespaces = configProp.getConfigValue("TenantListNameSpaces").split(",");
        Object[][] utilz = new Object[namespaces.length + 3][2];
        utilz[0][0] = "Label";
        utilz[0][1] = "Value";
        BigDecimal totalUtilization = new BigDecimal(0);
        int index = 0;
        for(String namespace:namespaces)
        {
            BigDecimal tenantCpu =  tenantComputeMemResource.get(namespace).get("cpu");

            BigDecimal tenantMem = tenantComputeMemResource.get(namespace).get("memory");

            BigDecimal cpuut = tenantCpu.divide(totalcpu, 2, RoundingMode.HALF_UP);
            BigDecimal memut = tenantMem.divide(totalmemory,2, RoundingMode.HALF_UP);
            BigDecimal finalut = cpuut.add(memut);
            index =index + 1;
            utilz[index][0] = configProp.getConfigValue(namespace + ".name") + "-CPU";
            utilz[index][1] = cpuut.multiply( new BigDecimal(100)).intValue();
            index =index + 1;
            utilz[index][0] = configProp.getConfigValue(namespace + ".name") + "-MEM";
            utilz[index][1] = memut.multiply( new BigDecimal(100)).intValue();


            BigDecimal utilization = finalut.divide(new BigDecimal(2),2, RoundingMode.HALF_UP);

            totalUtilization =  totalUtilization.add(utilization);
            usageData.put(namespace, utilization);
        }

        BigDecimal otherUtilization = new BigDecimal(1).subtract(totalUtilization);

        usageData.put("Others", otherUtilization);
        usageVo.setAvgusageData(usageData);
        usageVo.setTenantresourceallocation(utilz);

        return usageVo;
//        Map<String, BigDecimal> usageData = new HashMap<String, BigDecimal>();
//        usageData.put("tenantonenamespace", new BigDecimal("0.2"));
//        usageData.put("tenanttwonamespace", new BigDecimal("0.3"));
//        usageData.put("Others", new BigDecimal("0.5"));
//        usageVo.setAvgusageData(usageData);
//
//        Object[][] utilz = new Object[5][2];
//        utilz[0][0] = "Label";
//        utilz[0][1] = "Value";
//        utilz[1][0] = "TNT1-MEM";
//        utilz[1][1] = 25;
//        utilz[2][0] = "T_CPU-1";
//        utilz[2][1] = 6;
//        utilz[3][0] = "T2-MEM";
//        utilz[3][1] = 23;
//        utilz[4][0] = "T2-CPU";
//        utilz[4][1] = 8;
//        usageVo.setTenantresourceallocation(utilz);
//           return usageVo;

    }
}
