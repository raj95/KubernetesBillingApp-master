package kubernetes.billing;



import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;

import java.util.*;

public class Utils {
   static  HashMap<Integer, String> monthMap = new HashMap<Integer, String>();
   static{

       monthMap.put(1,"Jan");
       monthMap.put(2,"Feb");
       monthMap.put(3,"Mar");
       monthMap.put(4,"Apr");
       monthMap.put(5,"May");
       monthMap.put(6,"Jun");
       monthMap.put(7,"Jul");
       monthMap.put(8,"Aug");
       monthMap.put(9,"Sep");
       monthMap.put(10,"Oct");
       monthMap.put(11,"Nov");
       monthMap.put(12,"Dec");

   }

   public static void main(String[] args)
   {
       new Utils().getStartAndEndMonths(1);
   }


    public Map<Integer, ArrayList<String>> getStartAndEndMonths(int start) {
        HashMap<Integer, ArrayList<String>> monthsArray = new HashMap<Integer, ArrayList<String>> ();
       try {


           int year = Calendar.getInstance().get(Calendar.YEAR);
           String currentdate = "";
           for (int i = start; i <= getCurrentMonth(); i++) {
               ArrayList<String> dateList = new ArrayList<String>();
               if( i == getCurrentMonth())
               {
                    currentdate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                   if (i <10)
                   {
                       dateList.add(year + "-0" + i + "-01");
                       dateList.add(year + "-0" + i + "-" + lastDayOfMonth(year, i));
                   }
                   else
                   {
                       dateList.add(year + "-" + i + "-01");
                       dateList.add(year + "-" + i + "-" + lastDayOfMonth(year, i));
                   }
               }
               else
               {
                   if (i <10)
                   {
                       dateList.add(year + "-0" + i + "-01");
                       dateList.add(year + "-0" + i + "-" + lastDayOfMonth(year, i));
                   }
                   else
                   {
                       dateList.add(year + "-" + i + "-01");
                       dateList.add(year + "-" + i + "-" + lastDayOfMonth(year, i));
                   }
               }

               monthsArray.put(i, dateList);
           }

       }
       catch (Exception e)
       {

       }
       return monthsArray;
    }

    public int getCurrentMonth()
    {
        Date date = new Date();
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int month = localDate.getMonthValue();
        return month;
    }

    public int lastDayOfMonth(int Y, int M) {
        return LocalDate.of(Y, M, 1).getMonth().length(Year.of(Y).isLeap());
    }

}
