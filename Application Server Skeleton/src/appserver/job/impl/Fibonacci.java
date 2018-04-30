package appserver.job.impl;

//Tool that receive a number and return the corresponding Fibonacci

import appserver.job.Tool;

public class Fibonacci implements Tool {

   @Override
   public Object go(Object parameters) {
       Integer givenNumber = (Integer) parameters;
       //base case
       if (givenNumber.intValue()==1 || givenNumber.intValue() == 2)
       {
           return new Integer(1);
       }
       else {
           //if number is bigger than 2
           Integer firstPreviousFibo = (Integer) go(givenNumber-1);
           Integer secondPreviousFibo2 = (Integer) go(givenNumber-2);
           return new Integer(firstPreviousFibo+secondPreviousFibo2);
       }
   }
}
