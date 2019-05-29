package com.example.faudyhamka.userapp;


public class Number {
    public Number()
    {}

    public String[] number = new String[] {"-","-","-","-","-"};

    public void setNum(Integer i, String a) {this.number[i] = a;}

    public String getNum(Integer i) {return number[i];}
}
