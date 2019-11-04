package kubernetes.billing;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.costexplorer.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



@Component
public class AWSCostAndUsageAPI {

    @Autowired
    ConfigProperties configProp;

    @Autowired
    ResourceUsageAPI resourceUsageAPI;



    public Map<Integer, String> getTotalEKSCostData() {

        String eksTagName = configProp.getConfigValue("EKS-Tag-Name");
        String eksTagValue = configProp.getConfigValue("EKS-Tag-Value");
        Map<Integer, ArrayList<String>> map = new Utils().getStartAndEndMonths(1);
        Map<Integer, String> totaleksBillingData = new HashMap<>();
        for (int k : map.keySet()) {

            final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
                    .withTimePeriod(new DateInterval().withStart(map.get(k).get(0)).withEnd(map.get(k).get(1)))
                    .withGranularity(Granularity.MONTHLY)
                    .withMetrics("BlendedCost", "UsageQuantity")
                    .withFilter(new Expression().withDimensions(new DimensionValues().withKey("SERVICE")
                            .withValues("Amazon Elastic Container Service for Kubernetes")))
                    .withGroupBy(new GroupDefinition().withType("TAG").withKey(eksTagName));

            try {
                AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
                       // .withCredentials(getCredentials())
                        .build();

                GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);
                if (ceResult != null && ceResult.getResultsByTime() != null && ceResult.getResultsByTime().size() > 0)
                    ceResult.getResultsByTime().forEach(resultsByTime -> {
                        for (Group grp : resultsByTime.getGroups()) {
                            for (String grpkey : grp.getKeys())
                                if (grpkey.equals(eksTagName + "$" + eksTagValue))
                                    totaleksBillingData.put(k, grp.getMetrics().get("BlendedCost").getAmount());

                        }


                        System.out.println(resultsByTime.toString());

                    });


            } catch (final Exception e) {
                totaleksBillingData.put(k, "0");
                System.out.println(e);
            }

            if (totaleksBillingData.get(k) == null) {
                totaleksBillingData.put(k, "0");
            }

        }
        return totaleksBillingData;


    }

    public Map<Integer, String> getTotalEC2CostData() {


        String ec2TagName = configProp.getConfigValue("EC2-Tag-Name");
        String ec2TagValue = configProp.getConfigValue("EC2-Tag-Value");
        Map<Integer, ArrayList<String>> map = new Utils().getStartAndEndMonths(1);
        Map<Integer, String> totalec2BillingData = new HashMap<>();
        for (int k : map.keySet()) {

            final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
                    .withTimePeriod(new DateInterval().withStart(map.get(k).get(0)).withEnd(map.get(k).get(1)))
                    .withGranularity(Granularity.MONTHLY)
                    .withMetrics("BlendedCost", "UsageQuantity")
                    .withFilter(new Expression().withDimensions(new DimensionValues().withKey("SERVICE")
                            .withValues("Amazon Elastic Compute Cloud - Compute")))
                    .withGroupBy(new GroupDefinition().withType("TAG").withKey(ec2TagName));

            try {
                AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
                       // .withCredentials(getCredentials())
                        .build();

                GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);
                if (ceResult != null && ceResult.getResultsByTime() != null && ceResult.getResultsByTime().size() > 0)
                    ceResult.getResultsByTime().forEach(resultsByTime -> {
                        for (Group grp : resultsByTime.getGroups()) {
                            for (String grpkey : grp.getKeys())
                                if (grpkey.equals(ec2TagName + "$" + ec2TagValue))
                                    totalec2BillingData.put(k, grp.getMetrics().get("BlendedCost").getAmount());
                            // System.out.println("AMOUNT IS_->" + grp.getMetrics().get("BlendedCost").getAmount());
                        }


                        System.out.println(resultsByTime.toString());

                    });


            } catch (final Exception e) {
                totalec2BillingData.put(k, "0");
                System.out.println(e);
            }

            if (totalec2BillingData.get(k) == null) {
                totalec2BillingData.put(k, "0");
            }

        }
        return totalec2BillingData;


    }

    public Map<String, HashMap<String, String>> getCostData() {

        Map<String, HashMap<String, String>> totalcostdata = new HashMap<String, HashMap<String, String>>();
        Map<Integer, String> ec2Cost = getTotalEC2CostData();
        Map<Integer, String> eksCost = getTotalEKSCostData();

        Map<String, BigDecimal> usageData = resourceUsageAPI.getTenantUsageInPercentage().getAvgusageData();
        usageData.remove("Others");

        HashMap<String, String> totalcostmap = new HashMap<String, String>();
        HashMap<String, String> othercostmap = new HashMap<String, String>();
        totalcostdata.put("Total", totalcostmap);
        for (String name : usageData.keySet()) {
            HashMap<String, String> tenantcostmap = new HashMap<String, String>();
            for (Integer k : ec2Cost.keySet()) {
                BigDecimal totalCost = new BigDecimal(ec2Cost.get(k)).add(new BigDecimal(eksCost.get(k)));
                totalcostmap.put(Utils.monthMap.get(k), String.valueOf(totalCost));
                if (othercostmap.get(Utils.monthMap.get(k)) == null) {
                    othercostmap.put(Utils.monthMap.get(k), String.valueOf(totalCost));
                }

                int tenantStartMonth = Integer.valueOf(configProp.getConfigValue(name));


                if (k >= tenantStartMonth) {
                    BigDecimal tenantCost = totalCost.multiply(usageData.get(name));
                    tenantcostmap.put(Utils.monthMap.get(k), String.valueOf(tenantCost));
                    if (othercostmap.get(Utils.monthMap.get(k)) != null) {
                        BigDecimal residualCost = new BigDecimal(othercostmap.get(Utils.monthMap.get(k))).subtract(tenantCost);
                        othercostmap.put(Utils.monthMap.get(k), String.valueOf(residualCost));
                    }
                } else {
                    tenantcostmap.put(Utils.monthMap.get(k), "0");
                }

            }
            totalcostdata.put(name, tenantcostmap);
        }
        totalcostdata.put("Others", othercostmap);
        return totalcostdata;
    }


    public AWSStaticCredentialsProvider getCredentials() throws Exception {
        String costexplorerRoleArn = configProp.getConfigValue("CostExplorerRoleARN");
                AssumeRoleRequest assumeRole = new AssumeRoleRequest()
                .withRoleArn(costexplorerRoleArn)
                .withRoleSessionName("cost-explorer");

        AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder.standard().build();
        Credentials credentials = sts.assumeRole(assumeRole).getCredentials();

        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                credentials.getAccessKeyId(),
                credentials.getSecretAccessKey(),
                credentials.getSessionToken());

        return new AWSStaticCredentialsProvider(sessionCredentials);
    }

}