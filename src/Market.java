import java.util.ArrayList;

public class Market{
  private double stdDevVac,stdDevRG,stdDevCap,cvVacancy,cvRG,cvCap,betaVac,betaRG,betaCap;
  private ArrayList<Double> tenYrRG = new ArrayList<Double>();
  private ArrayList<Double> tenYrCap = new ArrayList<Double>();
  private ArrayList<Double> tenYrVac = new ArrayList<Double>();
  private String marketName;

  public Market(String marketName, double stdDevVac, double stdDevRG, double stdDevCap, double cvVacancy, double cvRG, double cvCap, double betaVac, double betaRG, double betaCap, ArrayList<Double> tenYrRG, ArrayList<Double> tenYrCap, ArrayList<Double> tenYrVac){
    this.marketName = marketName;
    this.stdDevVac = stdDevVac;
    this.stdDevRG = stdDevRG;
    this.stdDevCap = stdDevCap;
    this.cvVacancy = cvVacancy;
    this.cvRG = cvRG;
    this.cvCap = cvCap;
    this.betaVac = betaVac;
    this.betaRG = betaRG;
    this.betaCap = betaCap;
    this.tenYrRG = tenYrRG;
    this.tenYrCap = tenYrCap;
    this.tenYrVac = tenYrVac;
  }
  
  // Getters

  public ArrayList<Double> getTenYrRG(){
    return tenYrRG;
  }

  public ArrayList<Double> getTenYrCap(){
    return tenYrCap;
  }

  public ArrayList<Double> getTenYrVac(){
    return tenYrVac;
  }

  public String getMarketName(){
    return marketName;
  }
  public double getStdDevVac(){
    return stdDevVac;
  }

  public double getStdDevRG(){
    return stdDevRG;
  }

  public double getStdDevCap(){
    return stdDevCap;
  }

  public double getCVVacancy(){
    return cvVacancy;
  }

  public double getCVRG(){
    return cvRG;
  }

  public double getCVCap(){
    return cvCap;
  }

  public double getBetaVac(){
    return betaVac;
  }

  public double getBetaRG(){
    return betaRG;
  }

  public double getBetaCap(){
    return betaCap;
  }
  
  //Setters

  public void setTenYrRG(ArrayList<Double> tenYrRG){
    this.tenYrRG = tenYrRG;
  }

  public void setTenYrCap(ArrayList<Double> tenYrCap){
    this.tenYrCap = tenYrCap;
  }
  public void setTenYrVac(ArrayList<Double> tenYrVac){
    this.tenYrVac = tenYrVac;
  }

  public void setStdDevVac(double stdDevVac){
    this.stdDevVac = stdDevVac;
  }

  public void setStdDevRG(double stdDevRG){
    this.stdDevRG = stdDevRG;
  }

  public void setStdDevCap(double stdDevCap){
    this.stdDevCap = stdDevCap;
  }
  
  public void setCVVacancy(double cvVacancy){
    this.cvVacancy = cvVacancy;
  }

  public void setCVRG(double cvRG){
    this.cvRG = cvRG;
  }

  public void setCVCap(double cvCap){
    this.cvCap = cvCap;
  }

  public void setBetaVac(double betaVac){
    this.betaVac = betaVac;
  }

  public void setBetaRG(double betaRG){
    this.betaRG = betaRG;
  }

  public void setBetaCap(double betaCap){
    this.betaCap = betaCap;
  }

}
