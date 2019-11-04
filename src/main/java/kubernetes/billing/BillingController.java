package kubernetes.billing;


import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BillingController {

        @Autowired
        AWSCostAndUsageAPI costAndUsageAPI;

    @Autowired
    ResourceUsageAPI resourceUsageAPI;

         @Autowired
        ConfigProperties configProp;

    @RequestMapping("/")
    public String getHomePage(Model model) {
        return "home";
    }

        @RequestMapping("/eksresourceAndbilling")
        public String getResourceAndBillingData(Model model) {

            Map<String,  HashMap<String, String>> costmap =  costAndUsageAPI.getCostData();
            String tenants[] = configProp.getConfigValue("TenantListNames").split(",");
            String tenantNameSpaces[] = configProp.getConfigValue("TenantListNameSpaces").split(",");
           int numberoftenants = tenants.length + 3;

            int totalCostMaplength = costmap.get("Total").size();
            Object[][] result = new Object[totalCostMaplength + 1][numberoftenants];
            result[0][0] = "Month";
            result[0][1] = "Total";
            result[0][2] = "Others";
            int index = 0;
            for(int i = 3; i < tenants.length +3; i++)
            {

                result[0][i] = tenants[index];
                index = index + 1;
            }
            int totalPoints = tenants.length +3;

            for(int i =1; i<totalCostMaplength + 1; i++)
            {
                int j = 0;
                String month = Utils.monthMap.get(i);
                result[i][j] = month;
                j = j +1 ;
                HashMap totalCostMap = costmap.get("Total");
                Double totalCost = Double.valueOf((String)totalCostMap.get(month));
                result[i][j] = totalCost;

                j = j +1 ;
                HashMap otherCostMap = costmap.get("Others");
                Double otherCost = Double.valueOf((String)otherCostMap.get(month));

                result[i][j] = otherCost;

                int indexpt = 0;
                for(;j < totalPoints - 1;)
                {
                    j = j+1;
                    HashMap tenantCostMap = costmap.get(tenantNameSpaces[indexpt]);
                    Double tenantCost = Double.valueOf((String)tenantCostMap.get(month));
                    indexpt = indexpt + 1;
                    result[i][j] = tenantCost;
                }


            }
            Object[][] usageresult = new Object[tenants.length + 2][2];
            usageresult[0][0] = "Tenants";
            usageresult[0][1] = "ResourceAssigned" ;
            Map<String, BigDecimal> usagemap = resourceUsageAPI.getTenantUsageInPercentage().getAvgusageData();
            int rowdata = 1;

            for(String key:usagemap.keySet())
            {
                int columnData = 0;
                if(configProp.getConfigValue(key + ".name") != null)
                {
                    usageresult[rowdata][columnData] = configProp.getConfigValue(key + ".name");
                }

                else{
                    usageresult[rowdata][columnData] =key;

                }
                columnData = columnData + 1;
                usageresult[rowdata][columnData] = usagemap.get(key);
                rowdata = rowdata + 1;
            }
            model.addAttribute("billingdata", result);
           model.addAttribute("usagedata", usageresult);
           model.addAttribute("resourcedata", resourceUsageAPI.getTenantUsageInPercentage().getTenantresourceallocation());
            return "eksresourceandbillingdatainfo";


    }
}
